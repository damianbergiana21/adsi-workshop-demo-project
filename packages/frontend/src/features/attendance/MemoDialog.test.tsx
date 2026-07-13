/**
 * Feature #4: 備考 (memo) フィールド — Red テスト (Frontend)
 *
 * 受け入れ基準:
 * - 出勤/退勤ボタン押下でメモ入力ダイアログが表示される
 * - 200文字制限でリアルタイムカウンター表示（180文字超過時）
 * - 確認ボタンで memo 付き mutation が呼ばれる
 * - スキップ（空送信）で memo=undefined で mutation が呼ばれる
 *
 * 現時点では MemoDialog コンポーネントが存在しないため、
 * import で FAIL する（Red）。
 */
import { render, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { type Mock, beforeEach, describe, expect, it, vi } from "vitest";
// This import will fail until the component is created (Red)
import { MemoDialog } from "./MemoDialog";

describe("MemoDialog", () => {
  const mockOnConfirm = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("ダイアログが開いているときにtextareaが表示される", () => {
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const view = within(container);
    expect(view.getByPlaceholderText(/備考（任意）/)).toBeInTheDocument();
  });

  it("テキストエリアが3行表示（rows=3）である", () => {
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const textarea = container.querySelector("textarea");
    expect(textarea).toHaveAttribute("rows", "3");
  });

  it("確認ボタン押下でonConfirmがmemo付きで呼ばれる", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const view = within(container);

    await user.type(view.getByPlaceholderText(/備考（任意）/), "遅刻:電車遅延");
    await user.click(view.getByRole("button", { name: /確認|打刻/ }));

    expect(mockOnConfirm).toHaveBeenCalledWith("遅刻:電車遅延");
  });

  it("空のままスキップするとonConfirmがundefinedで呼ばれる", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const view = within(container);

    await user.click(view.getByRole("button", { name: /スキップ|確認|打刻/ }));

    expect(mockOnConfirm).toHaveBeenCalledWith(undefined);
  });

  it("200文字まで入力可能で、201文字目はブロックされる", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const textarea = container.querySelector("textarea") as HTMLTextAreaElement;

    const text200 = "あ".repeat(200);
    await user.type(textarea, text200);
    expect(textarea.value).toHaveLength(200);

    // 201文字目はブロック
    await user.type(textarea, "x");
    expect(textarea.value).toHaveLength(200);
  });

  it("180文字超過時に残り文字数カウンターが表示される", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const view = within(container);
    const textarea = container.querySelector("textarea") as HTMLTextAreaElement;

    // 180文字まではカウンター非表示
    const text180 = "あ".repeat(180);
    await user.type(textarea, text180);
    expect(view.queryByText(/残り/)).not.toBeInTheDocument();

    // 181文字目でカウンター表示
    await user.type(textarea, "い");
    expect(view.getByText(/残り.*19/)).toBeInTheDocument();
  });

  it("キャンセルボタンでonCancelが呼ばれる", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoDialog open={true} onConfirm={mockOnConfirm} onCancel={mockOnCancel} />,
    );
    const view = within(container);

    await user.click(view.getByRole("button", { name: /キャンセル|閉じる/ }));
    expect(mockOnCancel).toHaveBeenCalled();
  });
});
