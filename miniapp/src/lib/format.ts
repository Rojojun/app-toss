import type { ItemCategory } from "../api/types";

export const CATEGORIES: ReadonlyArray<{ readonly value: ItemCategory; readonly label: string }> = [
  { value: "TOILETRIES", label: "욕실" },
  { value: "CLEANING", label: "청소" },
  { value: "LAUNDRY", label: "세탁" },
  { value: "KITCHEN", label: "주방" },
  { value: "OTHER", label: "기타" },
];

export const categoryLabel = (category: ItemCategory): string =>
  CATEGORIES.find((entry) => entry.value === category)?.label ?? "기타";

const pad = (value: number): string => String(value).padStart(2, "0");

const sameDay = (a: Date, b: Date): boolean =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

// 오늘 14:02 / 어제 20:15 / 9월 28일
export const formatWhen = (iso: string, now: Date = new Date()): string => {
  const date = new Date(iso);
  const yesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;

  if (sameDay(date, now)) return `오늘 ${time}`;
  if (sameDay(date, yesterday)) return `어제 ${time}`;
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
};

// 10월 3일
export const formatDay = (iso: string): string => {
  const date = new Date(iso);
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
};
