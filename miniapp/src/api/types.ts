export type ItemCategory = "TOILETRIES" | "CLEANING" | "LAUNDRY" | "KITCHEN" | "OTHER";
export type HouseholdRole = "OWNER" | "MEMBER";

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: { id: string };
};

export type Me = {
  user: { id: string; appUserStatus: string; createdAt: string };
  households: ReadonlyArray<{ id: string; name: string; myRole: HouseholdRole }>;
};

export type Member = {
  householdId: string;
  userId: string;
  nickname: string;
  role: HouseholdRole;
  joinedAt: string;
};

export type HouseholdDetail = {
  id: string;
  name: string;
  myRole: HouseholdRole;
  myNickname: string;
  members: ReadonlyArray<Member>;
  createdAt: string;
};

export type Item = {
  id: string;
  householdId: string;
  name: string;
  category: ItemCategory;
  unit: string;
  quantity: number;
  lowStockThreshold: number;
  lowStock: boolean;
  version: number;
};

export type NewItem = {
  name: string;
  category: ItemCategory;
  unit: string;
  quantity: number;
  lowStockThreshold: number;
};

export type ItemPatch = {
  expectedVersion: number;
  name?: string;
  category?: ItemCategory;
  unit?: string;
  lowStockThreshold?: number;
};

export type StockChange = {
  id: string;
  itemId: string;
  actorUserId: string;
  actorNickname: string;
  beforeQuantity: number;
  afterQuantity: number;
  createdAt: string;
};

export type InvitationIssued = { id: string; token: string; expiresAt: string; maxUses: number };
export type InvitationPreview = { householdName: string; available: boolean };
export type Accepted = { householdId: string; role: HouseholdRole };
