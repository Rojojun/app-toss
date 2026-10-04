import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { getTossShareLink, share } from "@apps-in-toss/web-framework";
import { Badge, Button, ConfirmDialog, ListRow, SegmentedControl, TextButton, TextField, Top, useBottomSheet, useDialog, useToast } from "@toss/tds-mobile";
import {
  changeNickname,
  createInvitation,
  dissolveHousehold,
  leaveHousehold,
  removeMember,
  revokeInvitation,
  transferOwnership,
} from "../api/endpoints";
import type { InvitationIssued, Member } from "../api/types";
import { APP_NAME } from "../config";
import { Avatar } from "../components/CategoryIcon";
import { formatDay } from "../lib/format";
import { useCurrentHousehold, useHousehold } from "../hooks";
import { useSession } from "../session";

const HOURS = [1, 24, 168] as const;
const USES = [1, 3, 5, 10] as const;
const HOUR_LABEL: Record<number, string> = { 1: "1시간", 24: "1일", 168: "7일" };

function NicknameSheet({ initial, onSave }: { readonly initial: string; readonly onSave: (nickname: string) => void }) {
  const [value, setValue] = useState(initial);
  return (
    <div className="stack" style={{ paddingBottom: 24 }}>
      <TextField variant="box" label="내 이름" labelOption="sustain" value={value} maxLength={20} onChange={(e) => setValue(e.target.value)} />
      <div className="pad">
        <Button display="block" disabled={!value.trim()} onClick={() => onSave(value.trim())}>저장</Button>
      </div>
    </div>
  );
}

type MemberAction = { readonly label: string; readonly danger?: boolean; readonly onPick: () => void };

// 멤버 행의 ⋯ 를 누르면 아래에서 올라오는 행동 목록. 작은 팝업보다 누르기 쉽고 화면 위쪽 행에서도 잘리지 않는다.
function ActionSheet({ title, actions }: { readonly title: string; readonly actions: ReadonlyArray<MemberAction> }) {
  return (
    <div style={{ paddingBottom: 16 }}>
      <h3 className="sheet-title">{title}</h3>
      {actions.map((action) => (
        <ListRow
          key={action.label}
          onClick={action.onPick}
          contents={<ListRow.Texts type="1RowTypeA" top={<span style={{ color: action.danger ? "#f04452" : undefined }}>{action.label}</span>} />}
        />
      ))}
    </div>
  );
}

// 소유권을 넘길 사람을 고른다. 고른 뒤에 한 번 더 확인한다.
function TransferSheet({ members, onPick }: { readonly members: ReadonlyArray<Member>; readonly onPick: (member: Member) => void }) {
  return (
    <div style={{ paddingBottom: 16 }}>
      <h3 className="sheet-title">누구에게 넘길까요?</h3>
      <p className="muted pad" style={{ margin: "6px 0 8px" }}>넘기면 나는 일반 멤버가 돼요.</p>
      {members.map((member, index) => (
        <ListRow
          key={member.userId}
          onClick={() => onPick(member)}
          left={<Avatar name={member.nickname} seed={index} />}
          contents={<ListRow.Texts type="1RowTypeA" top={member.nickname} />}
          right={<span aria-hidden style={{ color: "#b0b8c1", fontSize: 20 }}>›</span>}
        />
      ))}
    </div>
  );
}

type Issued = { readonly invitation: InvitationIssued; readonly link: string; readonly hours: number };

// 8-a 만들기 → 8-b 완료. 만든 직후에만 전체 링크를 볼 수 있어서 같은 시트 안에서 두 단계로 보여 준다.
function InviteSheet({ householdId, householdName, onClose }: { readonly householdId: string; readonly householdName: string; readonly onClose: () => void }) {
  const { openToast } = useToast();
  const [hours, setHours] = useState<number>(24);
  const [uses, setUses] = useState<number>(3);
  const [issued, setIssued] = useState<Issued | null>(null);
  const [busy, setBusy] = useState(false);

  const issue = async () => {
    setBusy(true);
    try {
      const invitation = await createInvitation(householdId, hours, uses);
      const link = await getTossShareLink(`intoss://${APP_NAME}/invite?token=${encodeURIComponent(invitation.token)}`);
      setIssued({ invitation, link, hours });
    } catch {
      openToast("링크를 만들지 못했어요. 잠시 후 다시 시도해 주세요.");
    } finally {
      setBusy(false);
    }
  };

  const shareLink = (link: string) =>
    share({ message: `'${householdName}'에 초대해요. 같이 남은 수량을 확인해요! ${link}` }).catch(() => openToast("공유하지 못했어요."));

  const copy = (link: string) =>
    navigator.clipboard.writeText(link).then(
      () => openToast("링크를 복사했어요."),
      () => openToast("복사하지 못했어요."),
    );

  const revoke = async (id: string) => {
    await revokeInvitation(householdId, id).catch(() => openToast("취소하지 못했어요."));
    onClose();
  };

  if (issued)
    return (
      <div className="stack" style={{ paddingBottom: 24 }}>
        <h3 className="sheet-title">링크가 만들어졌어요</h3>
        <p className="muted pad" style={{ margin: 0 }}>{HOUR_LABEL[issued.hours]} 동안, 최대 {issued.invitation.maxUses}명이 참여할 수 있어요.</p>
        <div className="link-box">{issued.link}</div>
        <div className="pad row">
          <div className="grow"><Button display="block" onClick={() => shareLink(issued.link)}>공유하기</Button></div>
          <Button variant="weak" onClick={() => copy(issued.link)}>복사</Button>
        </div>
        <div className="foot-link">
          <TextButton size="small" color="#f04452" onClick={() => revoke(issued.invitation.id)}>이 링크 취소하기</TextButton>
        </div>
      </div>
    );

  return (
    <div className="stack" style={{ paddingBottom: 24 }}>
      <h3 className="sheet-title">초대 링크 만들기</h3>
      <p className="muted pad" style={{ margin: 0 }}>링크를 받은 사람은 별도 가입 없이 참여할 수 있어요.</p>
      <div className="pad stack" style={{ gap: 8 }}>
        <span className="muted">유효 시간</span>
        <SegmentedControl alignment="fluid" value={String(hours)} onChange={(v) => setHours(Number(v))}>
          {HOURS.map((h) => <SegmentedControl.Item key={h} value={String(h)}>{HOUR_LABEL[h]}</SegmentedControl.Item>)}
        </SegmentedControl>
        <span className="muted">참여할 수 있는 사람 수</span>
        <SegmentedControl alignment="fluid" value={String(uses)} onChange={(v) => setUses(Number(v))}>
          {USES.map((u) => <SegmentedControl.Item key={u} value={String(u)}>{u}명</SegmentedControl.Item>)}
        </SegmentedControl>
      </div>
      <div className="pad"><Button display="block" loading={busy} onClick={issue}>링크 만들기</Button></div>
    </div>
  );
}

export function MembersPage({ openInvite = false }: { readonly openInvite?: boolean }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { openToast } = useToast();
  const { openConfirm } = useDialog();
  const sheet = useBottomSheet();
  const session = useSession();
  const { household } = useCurrentHousehold();
  const detail = useHousehold(household?.id).data;
  const opened = useRef(false);

  const refresh = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ["household", household?.id] }),
    queryClient.invalidateQueries({ queryKey: ["me"] }),
  ]);
  const act = useMutation({
    mutationFn: (run: () => Promise<unknown>) => run(),
    onSuccess: refresh,
    onError: () => openToast("처리하지 못했어요. 잠시 후 다시 시도해 주세요."),
  });

  const showInvite = () =>
    detail && sheet.open({
      children: <InviteSheet householdId={detail.id} householdName={detail.name} onClose={sheet.close} />,
      onClose: sheet.close,
    });

  useEffect(() => {
    if (openInvite && detail && !opened.current) {
      opened.current = true;
      showInvite();
    }
  }, [openInvite, detail]); // eslint-disable-line react-hooks/exhaustive-deps

  const showNickname = () =>
    sheet.open({
      header: "내 이름 바꾸기",
      children: <NicknameSheet initial={detail?.myNickname ?? ""} onSave={(nickname) => { sheet.close(); act.mutate(() => changeNickname(detail!.id, nickname)); }} />,
      onClose: sheet.close,
    });

  const confirm = async (title: string, description: React.ReactNode, confirmText: string, danger: boolean, run: () => Promise<unknown>, after?: () => void) => {
    const ok = await openConfirm({
      title,
      description,
      cancelButton: <ConfirmDialog.CancelButton>취소</ConfirmDialog.CancelButton>,
      confirmButton: <ConfirmDialog.ConfirmButton color={danger ? "danger" : "primary"}>{confirmText}</ConfirmDialog.ConfirmButton>,
    });
    if (ok) act.mutate(run, { onSuccess: after });
  };

  const goHomeAfterExit = async () => {
    await queryClient.invalidateQueries({ queryKey: ["me"] });
    navigate("/", { replace: true });
  };

  if (!detail || session.status !== "ready") return <div className="page" />;
  const isOwner = detail.myRole === "OWNER";
  const alone = detail.members.length === 1;
  const others = detail.members.filter((member) => member.userId !== session.userId);

  const askTransfer = (member: Member) =>
    confirm(`${member.nickname}에게 소유권을 넘길까요?`, "넘긴 뒤에는 내가 일반 멤버가 돼요. 초대 링크 만들기와 멤버 관리는 상대만 할 수 있어요.", "넘기기", false, () => transferOwnership(detail.id, member.userId));
  const askRemove = (member: Member) =>
    confirm(`멤버 ‘${member.nickname}’을 내보낼까요?`, "내보내면 이 공간의 물품을 볼 수 없어요. 다시 들어오려면 새 초대 링크가 필요해요.", "내보내기", true, () => removeMember(detail.id, member.userId));

  const askLeave = () =>
    confirm(`${detail.name}에서 나갈까요?`, "나가면 이 공간의 물품을 볼 수 없어요. 다시 들어오려면 새 초대 링크가 필요해요.", "나가기", true, () => leaveHousehold(detail.id), goHomeAfterExit);
  const askDissolve = () =>
    confirm("공간을 해산할까요?", <>물품, 수량 변경 이력, 초대 링크가 모두 삭제돼요. <b style={{ color: "#f04452" }}>되돌릴 수 없어요.</b></>, "해산하기", true, () => dissolveHousehold(detail.id), goHomeAfterExit);
  const pickTransfer = () =>
    sheet.open({
      children: <TransferSheet members={others} onPick={(member) => { sheet.close(); void askTransfer(member); }} />,
      onClose: sheet.close,
    });

  const actionsFor = (member: Member, mine: boolean): ReadonlyArray<MemberAction> =>
    mine
      ? [
          { label: "이름 바꾸기", onPick: showNickname },
          ...(isOwner && !alone ? [{ label: "소유권 넘기기", onPick: pickTransfer }] : []),
          ...(isOwner && alone ? [{ label: "공간 해산", danger: true, onPick: askDissolve }] : []),
          ...(!isOwner ? [{ label: "공간 나가기", danger: true, onPick: askLeave }] : []),
        ]
      : isOwner
        ? [
            { label: "소유자로 지정", onPick: () => askTransfer(member) },
            { label: "내보내기", danger: true, onPick: () => askRemove(member) },
          ]
        : [];

  const menuFor = (member: Member, mine: boolean) => {
    const actions = actionsFor(member, mine);
    if (actions.length === 0) return null;
    return (
      <button
        type="button"
        className="member-btn"
        style={{ background: "transparent" }}
        aria-label="더보기"
        onClick={() =>
          sheet.open({
            children: (
              <ActionSheet
                title={member.nickname}
                actions={actions.map((action) => ({ ...action, onPick: () => { sheet.close(); action.onPick(); } }))}
              />
            ),
            onClose: sheet.close,
          })
        }
      >
        ⋯
      </button>
    );
  };

  return (
    <div className="page">
      <Top title={<Top.TitleParagraph>멤버 {detail.members.length}명</Top.TitleParagraph>} />
      {detail.members.map((member, index) => {
        const mine = member.userId === session.userId;
        return (
          <ListRow
            key={member.userId}
            left={<Avatar name={member.nickname} seed={index} />}
            contents={
              <ListRow.Texts
                type="2RowTypeA"
                bottom={`${formatDay(member.joinedAt)} 참여`}
                top={
                  <span className="row">
                    {member.nickname}
                    {member.role === "OWNER" && <Badge size="small" color="blue" variant="weak">소유자</Badge>}
                    {mine && <Badge size="small" color="elephant" variant="weak">나</Badge>}
                  </span>
                }
              />
            }
            right={menuFor(member, mine)}
          />
        );
      })}
      {isOwner && (
        <>
          <div className="divider" />
          <ListRow
            onClick={showInvite}
            contents={<ListRow.Texts type="2RowTypeA" top="초대 링크 만들기" bottom="가족이나 룸메이트를 초대해요" />}
            right={<span aria-hidden style={{ color: "#b0b8c1", fontSize: 20 }}>›</span>}
          />
        </>
      )}
      <div className="divider" />
      {isOwner && !alone && (
        <>
          <p className="note">소유자는 소유권을 다른 멤버에게 넘긴 뒤에 공간을 나갈 수 있어요.</p>
          <div className="foot-link">
            <TextButton size="medium" color="#f04452" onClick={pickTransfer}>소유권 넘기기…</TextButton>
          </div>
        </>
      )}
      {isOwner && alone && (
        <div className="foot-link" style={{ paddingTop: 20 }}>
          <TextButton size="medium" color="#f04452" onClick={askDissolve}>
            공간 해산
          </TextButton>
        </div>
      )}
      {!isOwner && (
        <div className="foot-link" style={{ paddingTop: 20 }}>
          <TextButton size="medium" color="#f04452" onClick={askLeave}>
            나가기
          </TextButton>
        </div>
      )}
    </div>
  );
}
