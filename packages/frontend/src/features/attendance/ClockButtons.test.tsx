import { render, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { type Mock, beforeEach, describe, expect, it, vi } from "vitest";
import { ClockButtons } from "./ClockButtons";
import { useClockIn, useClockOut, useTodayStatus } from "./useAttendance";

vi.mock("./useAttendance", () => ({
  useTodayStatus: vi.fn(),
  useClockIn: vi.fn(),
  useClockOut: vi.fn(),
}));

vi.mock("@/components/ui/skeleton", () => ({
  Skeleton: (props: Record<string, unknown>) =>
    createElement("div", { "data-testid": "skeleton", ...props }),
}));

const mockClockInMutate = vi.fn();
const mockClockOutMutate = vi.fn();

function setupMocks(
  status: "NOT_CLOCKED_IN" | "CLOCKED_IN" | "CLOCKED_OUT",
  overrides: { clockInPending?: boolean; clockOutPending?: boolean } = {},
) {
  (useTodayStatus as Mock).mockReturnValue({
    data: {
      status,
      records:
        status === "NOT_CLOCKED_IN"
          ? []
          : [{ id: "r1", workDate: "2026-07-13", clockIn: "2026-07-13T09:00:00", clockOut: null, corrected: false }],
    },
    isLoading: false,
  });
  (useClockIn as Mock).mockReturnValue({
    mutate: mockClockInMutate,
    isPending: overrides.clockInPending ?? false,
  });
  (useClockOut as Mock).mockReturnValue({
    mutate: mockClockOutMutate,
    isPending: overrides.clockOutPending ?? false,
  });
}

describe("ClockButtons", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("出勤済み（CLOCKED_IN）の状態", () => {
    it("出勤ボタンが無効化される（Bug #1: 現在は有効のままになっている）", () => {
      setupMocks("CLOCKED_IN");
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockInButton = view.getByRole("button", { name: /出勤/ });

      // BUG: canClockIn は status === "CLOCKED_IN" でも true を返すため、
      // このアサーションは現在 FAIL する（Red）
      expect(clockInButton).toBeDisabled();
    });

    it("退勤ボタンが有効である", () => {
      setupMocks("CLOCKED_IN");
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockOutButton = view.getByRole("button", { name: /退勤/ });
      expect(clockOutButton).toBeEnabled();
    });

    it("出勤ボタンを押しても mutate が呼ばれない", async () => {
      setupMocks("CLOCKED_IN");
      const user = userEvent.setup();
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockInButton = view.getByRole("button", { name: /出勤/ });
      await user.click(clockInButton);

      // BUG: ボタンが有効なのでクリックでき、mutate が呼ばれてしまう
      expect(mockClockInMutate).not.toHaveBeenCalled();
    });
  });

  describe("未出勤（NOT_CLOCKED_IN）の状態", () => {
    it("出勤ボタンが有効である", () => {
      setupMocks("NOT_CLOCKED_IN");
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockInButton = view.getByRole("button", { name: /出勤/ });
      expect(clockInButton).toBeEnabled();
    });

    it("退勤ボタンが無効化される", () => {
      setupMocks("NOT_CLOCKED_IN");
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockOutButton = view.getByRole("button", { name: /退勤/ });
      expect(clockOutButton).toBeDisabled();
    });
  });

  describe("退勤済み（CLOCKED_OUT）の状態", () => {
    it("出勤ボタンが無効化される", () => {
      setupMocks("CLOCKED_OUT");
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockInButton = view.getByRole("button", { name: /出勤/ });
      expect(clockInButton).toBeDisabled();
    });

    it("退勤ボタンが無効化される", () => {
      setupMocks("CLOCKED_OUT");
      const { container } = render(<ClockButtons />);
      const view = within(container);

      const clockOutButton = view.getByRole("button", { name: /退勤/ });
      expect(clockOutButton).toBeDisabled();
    });
  });
});
