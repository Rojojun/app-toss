#!/usr/bin/env python3
"""Convert this application's mysqldump into transactional PostgreSQL data SQL.

No database connections. Run Flyway's PostgreSQL migrations before importing.
MySQL Flyway history is deliberately excluded. Input values are never logged.
"""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import uuid

TABLES = {
    'app_user': ('id', '생성일', '익명 유저 키', '유저 상태'),
    'household': ('id', '생성일', 'created_by', 'name'),
    'household_member': ('household_id', 'user_id', 'joined_at', 'nickname', 'role'),
    'invitation': ('id', '생성일', 'household_id', 'token_hash', 'max_uses', 'used_count', 'expires_at', 'revoked_at', 'version'),
    'item': ('id', '생성일', 'household_id', 'name', 'category', 'unit', 'quantity', 'low_stock_threshold', 'version'),
    'stock_change': ('id', '생성일', 'item_id', 'actor_user_id', 'actor_nickname', 'before_quantity', 'after_quantity'),
    'alert_preference': ('household_id', 'user_id', 'enabled'),
    'low_stock_event': ('id', '생성일', 'household_id', 'item_id', 'item_version'),
    'out_of_stock_alert': ('id', '생성일', 'household_id', 'item_id', 'item_name', 'actor_user_id', 'due_at', 'reminders_sent'),
}
INSTANTS = {'joined_at', 'expires_at', 'revoked_at', 'due_at'}
DATES = INSTANTS | {'생성일'}
UUID_COLUMNS = {'id', 'household_id', 'user_id', 'created_by', 'item_id', 'actor_user_id'}
ESCAPES = {'0': '\0', 'b': '\b', 'n': '\n', 'r': '\r', 't': '\t', 'Z': '\x1a', '\\': '\\', "'": "'", '"': '"'}


def parse_rows(source):
    """Lex only mysqldump literal VALUES; never execute source SQL."""
    i = 0
    size = len(source)
    def skip():
        nonlocal i
        while i < size and source[i].isspace():
            i += 1
    def expect(char):
        nonlocal i
        skip()
        if i >= size or source[i] != char:
            raise ValueError('Unexpected VALUES syntax')
        i += 1
    while True:
        expect('(')
        row = []
        while True:
            skip()
            if i < size and source[i] == "'":
                i += 1
                value = []
                while i < size:
                    char = source[i]
                    i += 1
                    if char == '\\':
                        if i == size:
                            raise ValueError('Truncated string escape')
                        escaped = source[i]
                        i += 1
                        if escaped not in ESCAPES:
                            raise ValueError('Unsupported MySQL string escape')
                        value.append(ESCAPES[escaped])
                    elif char == "'":
                        if i < size and source[i] == "'":
                            value.append("'")
                            i += 1
                        else:
                            break
                    else:
                        value.append(char)
                else:
                    raise ValueError('Unterminated string')
                row.append(''.join(value))
            else:
                start = i
                while i < size and source[i] not in ',)':
                    i += 1
                token = source[start:i].strip()
                if token == 'NULL':
                    row.append(None)
                elif re.fullmatch(r'0x[0-9a-fA-F]+', token):
                    row.append(bytes.fromhex(token[2:]))
                elif re.fullmatch(r'-?\d+', token):
                    row.append(int(token))
                elif re.fullmatch(r"b'[01]+'", token):
                    row.append(int(token[2:-1], 2))
                else:
                    raise ValueError('Unsupported SQL value; dump with --hex-blob')
            skip()
            if i < size and source[i] == ',':
                i += 1
            else:
                expect(')')
                break
        yield row
        skip()
        if i == size:
            break
        expect(',')


def quote(value):
    if '\0' in value:
        raise ValueError('PostgreSQL text cannot contain NUL')
    return "'" + value.replace("'", "''") + "'"


def sql_value(column, value, instant_zone):
    if value is None:
        return 'NULL'
    if column in UUID_COLUMNS:
        if not isinstance(value, bytes) or len(value) != 16:
            raise ValueError('Expected 16-byte UUID')
        return quote(str(uuid.UUID(bytes=value)))
    if column == 'enabled':
        n = int.from_bytes(value, 'big') if isinstance(value, bytes) else value
        if n not in (0, 1):
            raise ValueError('Expected boolean')
        return 'TRUE' if n else 'FALSE'
    if column in DATES:
        datetime.fromisoformat(value)
        literal = quote(value)
        return f'({literal}::timestamp AT TIME ZONE {quote(instant_zone)})' if column in INSTANTS else literal
    if isinstance(value, str):
        return quote(value)
    if type(value) is int:
        return str(value)
    raise ValueError('Unexpected value type')


def convert(source, instant_zone):
    if not re.search(r'-- Dump completed on [^\n]+\s*$', source):
        raise ValueError('Missing mysqldump completion marker')
    schemas = {}
    for table, ddl in re.findall(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=[^;]+;', source, re.S):
        schemas[table] = tuple(re.findall(r'^\s*`([^`]+)`\s+', ddl, re.M))
    if set(schemas) != set(TABLES) | {'flyway_schema_history'}:
        raise ValueError('Unexpected source tables; review schema before migrating')
    for table, columns in TABLES.items():
        if schemas[table] != columns:
            raise ValueError(f'Unexpected columns in {table}')
    rows = {table: [] for table in TABLES}
    insert_count = 0
    for match in re.finditer(r'^INSERT INTO `([^`]+)` VALUES (.*);\s*$', source, re.M):
        insert_count += 1
        table, data = match.groups()
        if table == 'flyway_schema_history':
            continue
        if table not in TABLES:
            raise ValueError('Unexpected insert table')
        for row in parse_rows(data):
            if len(row) != len(TABLES[table]):
                raise ValueError(f'Wrong column count in {table}')
            rows[table].append(row)
    if insert_count != len(re.findall(r'^INSERT INTO ', source, re.M)):
        raise ValueError('Unrecognized INSERT syntax')
    lines = [
        '-- Generated PostgreSQL data import; apply Flyway migrations first.',
        '-- This is a snapshot. Stop source writes and take a fresh dump for final cutover.',
        'BEGIN;',
        'SET LOCAL standard_conforming_strings = on;',
        "SET LOCAL TIME ZONE 'UTC';",
        'LOCK TABLE ' + ', '.join('"' + t + '"' for t in TABLES) + ' IN ACCESS EXCLUSIVE MODE;',
        'DO $migration$ BEGIN',
    ]
    for table in TABLES:
        lines.append(f'IF EXISTS (SELECT 1 FROM "{table}") THEN RAISE EXCEPTION \'Target table {table} must be empty\'; END IF;')
    lines.append('END $migration$;')
    for table, columns in TABLES.items():
        if not rows[table]:
            continue
        header = ', '.join('"' + c + '"' for c in columns)
        for row in rows[table]:
            values = ', '.join(sql_value(c, v, instant_zone) for c, v in zip(columns, row))
            lines.append(f'INSERT INTO "{table}" ({header}) VALUES ({values});')
    lines.append('DO $migration$ BEGIN')
    for table in TABLES:
        lines.append(f'IF (SELECT count(*) FROM "{table}") <> {len(rows[table])} THEN RAISE EXCEPTION \'Row count mismatch: {table}\'; END IF;')
    # Check logical relationships even where the application schema has no FK.
    relations = [
        ('invitation', 'household_id', 'household'), ('item', 'household_id', 'household'),
        ('stock_change', 'item_id', 'item'), ('stock_change', 'actor_user_id', 'app_user'),
        ('alert_preference', 'household_id', 'household'), ('alert_preference', 'user_id', 'app_user'),
        ('low_stock_event', 'household_id', 'household'), ('low_stock_event', 'item_id', 'item'),
        ('out_of_stock_alert', 'household_id', 'household'), ('out_of_stock_alert', 'item_id', 'item'),
        ('out_of_stock_alert', 'actor_user_id', 'app_user'),
    ]
    for child, col, parent in relations:
        lines.append(f'IF EXISTS (SELECT 1 FROM "{child}" c LEFT JOIN "{parent}" p ON c."{col}" = p.id WHERE p.id IS NULL) THEN RAISE EXCEPTION \'Orphan reference: {child}.{col}\'; END IF;')
    lines += ['END $migration$;', 'COMMIT;', '']
    return '\n'.join(lines), {t: len(r) for t, r in rows.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('dump', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--instant-zone', required=True, help='Time zone in which Hibernate stored MySQL DATETIME Instant values; usually UTC. Verify source first.')
    args = parser.parse_args()
    source = args.dump.read_text(encoding='utf-8')
    sql, counts = convert(source, args.instant_zone)
    # Refuse overwrite and create with private permissions, regardless of umask.
    with os.fdopen(os.open(args.output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as f:
        f.write(sql)
    print(json.dumps({'source_sha256': hashlib.sha256(source.encode()).hexdigest(), 'instant_zone': args.instant_zone, 'rows': counts}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
