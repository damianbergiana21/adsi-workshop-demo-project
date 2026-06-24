import { fireEvent, render, screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { MonthSelector } from "./MonthSelector";

vi.mock("@/components/ui/button", () => ({
  Button: (props: Record<string, unknown>) => {
    const { variant, size, children, ...rest } = props;
    return createElement("button", { type: "button", ...rest }, children as string);
  },
}));

describe("MonthSelector", () => {
  it("年月を表示する", () => {
    render(<MonthSelector year={2024} month={6} onChange={vi.fn()} />);
    expect(screen.getByText("2024年6月")).toBeInTheDocument();
  });

  it("次月ボタンで月が進む", () => {
    const onChange = vi.fn();
    const { container } = render(<MonthSelector year={2024} month={6} onChange={onChange} />);

    const buttons = container.querySelectorAll("button");
    fireEvent.click(buttons[1]);

    expect(onChange).toHaveBeenCalledWith(2024, 7);
  });

  it("前月ボタンで月が戻る", () => {
    const onChange = vi.fn();
    const { container } = render(<MonthSelector year={2024} month={6} onChange={onChange} />);

    const buttons = container.querySelectorAll("button");
    fireEvent.click(buttons[0]);

    expect(onChange).toHaveBeenCalledWith(2024, 5);
  });

  it("1月で前月を押すと前年の12月になる", () => {
    const onChange = vi.fn();
    const { container } = render(<MonthSelector year={2024} month={1} onChange={onChange} />);

    const buttons = container.querySelectorAll("button");
    fireEvent.click(buttons[0]);

    expect(onChange).toHaveBeenCalledWith(2023, 12);
  });

  it("12月で次月を押すと翌年の1月になる", () => {
    const onChange = vi.fn();
    const { container } = render(<MonthSelector year={2024} month={12} onChange={onChange} />);

    const buttons = container.querySelectorAll("button");
    fireEvent.click(buttons[1]);

    expect(onChange).toHaveBeenCalledWith(2025, 1);
  });
});
