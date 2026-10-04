import { request } from "./client";
import type {
  Accepted,
  AuthResponse,
  HouseholdDetail,
  InvitationIssued,
  InvitationPreview,
  Item,
  ItemPatch,
  Me,
  NewItem,
  StockChange,
} from "./types";

const enc = encodeURIComponent;

// 인증
export const exchange = (code: string) => request<AuthResponse>("/auth/toss/exchange", { method: "POST", body: { code } });
export const getMe = () => request<Me>("/me");

// 공간
export const createHousehold = (name: string, nickname: string) =>
  request<HouseholdDetail>("/households", { method: "POST", body: { name, nickname } });
export const getHousehold = (id: string) => request<HouseholdDetail>(`/households/${id}`);
export const renameHousehold = (id: string, name: string) =>
  request<HouseholdDetail>(`/households/${id}`, { method: "PATCH", body: { name } });
export const changeNickname = (id: string, nickname: string) =>
  request<HouseholdDetail>(`/households/${id}/membership`, { method: "PATCH", body: { nickname } });
export const transferOwnership = (id: string, newOwnerUserId: string) =>
  request<HouseholdDetail>(`/households/${id}/ownership-transfer`, { method: "POST", body: { newOwnerUserId } });
export const dissolveHousehold = (id: string) => request<void>(`/households/${id}`, { method: "DELETE" });
export const leaveHousehold = (id: string) => request<void>(`/households/${id}/membership`, { method: "DELETE" });
export const removeMember = (id: string, userId: string) =>
  request<void>(`/households/${id}/members/${userId}`, { method: "DELETE" });

// 초대
export const createInvitation = (householdId: string, expiresInHours: number, maxUses: number) =>
  request<InvitationIssued>(`/households/${householdId}/invitations`, { method: "POST", body: { expiresInHours, maxUses } });
export const revokeInvitation = (householdId: string, invitationId: string) =>
  request<void>(`/households/${householdId}/invitations/${invitationId}`, { method: "DELETE" });
export const previewInvitation = (token: string) => request<InvitationPreview>(`/invitations/${enc(token)}`);
export const acceptInvitation = (token: string, nickname: string) =>
  request<Accepted>(`/invitations/${enc(token)}/accept`, { method: "POST", body: { nickname } });

// 물품
export const listItems = (householdId: string) =>
  request<{ items: ReadonlyArray<Item> }>(`/households/${householdId}/items`).then((response) => response.items);
export const createItem = (householdId: string, item: NewItem) =>
  request<Item>(`/households/${householdId}/items`, { method: "POST", body: item });
export const updateItem = (householdId: string, itemId: string, patch: ItemPatch) =>
  request<Item>(`/households/${householdId}/items/${itemId}`, { method: "PATCH", body: patch });
export const changeStock = (householdId: string, itemId: string, expectedVersion: number, newQuantity: number) =>
  request<{ item: Item; stockChange: StockChange }>(`/households/${householdId}/items/${itemId}/stock-changes`, {
    method: "POST",
    body: { expectedVersion, newQuantity },
  });
export const listStockChanges = (householdId: string, itemId: string) =>
  request<{ stockChanges: ReadonlyArray<StockChange> }>(`/households/${householdId}/items/${itemId}/stock-changes`).then(
    (response) => response.stockChanges,
  );
