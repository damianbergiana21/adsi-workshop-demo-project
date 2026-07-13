"use client";

import { useState } from "react";

interface MemoDialogProps {
  open: boolean;
  onConfirm: (memo: string | undefined) => void;
  onCancel: () => void;
}

const MAX_LENGTH = 200;
const COUNTER_THRESHOLD = 180;

export function MemoDialog({ open, onConfirm, onCancel }: MemoDialogProps) {
  const [value, setValue] = useState("");

  if (!open) return null;

  const remaining = MAX_LENGTH - value.length;
  const showCounter = value.length > COUNTER_THRESHOLD;

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const input = e.target.value;
    if (input.length <= MAX_LENGTH) {
      setValue(input);
    }
  };

  const handleConfirm = () => {
    const trimmed = value.trim();
    onConfirm(trimmed || undefined);
    setValue("");
  };

  const handleCancel = () => {
    setValue("");
    onCancel();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg dark:bg-gray-900">
        <h3 className="mb-4 text-lg font-semibold">打刻確認</h3>
        <div className="space-y-2">
          <textarea
            rows={3}
            maxLength={MAX_LENGTH}
            value={value}
            onChange={handleChange}
            placeholder="備考（任意）：遅刻理由、直行直帰など"
            className="w-full rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {showCounter && (
            <p className="text-right text-xs text-muted-foreground">
              残り{remaining}文字
            </p>
          )}
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <button
            type="button"
            onClick={handleCancel}
            className="rounded-md px-4 py-2 text-sm text-gray-600 hover:bg-gray-100"
          >
            キャンセル
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            className="rounded-md bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600"
          >
            打刻
          </button>
        </div>
      </div>
    </div>
  );
}
