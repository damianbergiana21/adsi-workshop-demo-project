import { ApiClientError } from "@/lib/api-client";
import { render, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { type Mock, beforeEach, describe, expect, it, vi } from "vitest";
import { LoginForm } from "./LoginForm";
import { useLogin } from "./useAuth";

vi.mock("./useAuth", () => ({
  useLogin: vi.fn(),
}));

vi.mock("@/components/ui/button", () => ({
  Button: (props: Record<string, unknown>) => {
    const { variant, size, children, ...rest } = props;
    return createElement("button", { type: "button", ...rest }, children as string);
  },
}));

vi.mock("@/components/ui/input", () => ({
  Input: (props: Record<string, unknown>) => {
    const { ref, ...rest } = props;
    return createElement("input", rest);
  },
}));

const mockMutate = vi.fn();

function setupMockLogin(overrides: Partial<ReturnType<typeof useLogin>> = {}) {
  (useLogin as Mock).mockReturnValue({
    mutate: mockMutate,
    isPending: false,
    error: null,
    ...overrides,
  });
}

describe("LoginForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupMockLogin();
  });

  it("メールアドレスとパスワードの入力欄がある", () => {
    const { container } = render(<LoginForm />);
    const view = within(container);

    expect(view.getByPlaceholderText("example@company.com")).toBeInTheDocument();
    expect(container.querySelector('input[type="password"]')).toBeInTheDocument();
    expect(view.getByRole("button", { name: "ログイン" })).toBeInTheDocument();
  });

  it("フォーム送信でmutateが呼ばれる", async () => {
    const { container } = render(<LoginForm />);
    const view = within(container);
    const user = userEvent.setup();

    await user.type(view.getByPlaceholderText("example@company.com"), "test@example.com");
    await user.type(container.querySelector('input[type="password"]') as HTMLElement, "password123");
    await user.click(view.getByRole("button", { name: "ログイン" }));

    expect(mockMutate).toHaveBeenCalledWith({
      email: "test@example.com",
      password: "password123",
    });
  });

  it("ローディング中はボタンが無効化される", () => {
    setupMockLogin({ isPending: true });
    const { container } = render(<LoginForm />);
    const view = within(container);

    expect(view.getByRole("button", { name: "ログイン中..." })).toBeDisabled();
  });

  it("ApiClientErrorの場合はdetailを表示する", () => {
    const error = new ApiClientError(
      401,
      "Unauthorized",
      "メールアドレスまたはパスワードが正しくありません",
    );
    setupMockLogin({ error });
    const { container } = render(<LoginForm />);
    const view = within(container);

    expect(
      view.getByText("メールアドレスまたはパスワードが正しくありません"),
    ).toBeInTheDocument();
  });

  it("想定外のエラーの場合は汎用メッセージを表示する", () => {
    setupMockLogin({ error: new Error("network error") });
    const { container } = render(<LoginForm />);
    const view = within(container);

    expect(view.getByText("ログインに失敗しました")).toBeInTheDocument();
  });
});
