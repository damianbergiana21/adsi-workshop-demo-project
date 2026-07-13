# Feature: 備考 (memo) フィールド — 仕様 Q&A

> Issue: #4
> ステータス: 回答待ち（全 [Answer] が埋まったら Issue に「Answers completed」とコメントしてください）

---

## 1. 基本仕様

### 1.1 必須/任意

[Question] 備考フィールドは必須ですか？任意ですか？
[Answer]

### 1.2 文字数制限

[Question] 備考の最大文字数は何文字にしますか？（候補: 50 / 100 / 200 / 500）
[Answer]

### 1.3 適用範囲

[Question] 備考を入力できるのは出勤時のみ？退勤時のみ？それとも両方？
[Answer]

### 1.4 編集・削除

[Question] 打刻後に備考を編集・削除できますか？できる場合の条件は？（例: 当日中のみ、管理者のみ、修正申請経由のみ）
[Answer]

### 1.5 文字種バリデーション

[Question] 入力可能な文字種に制限はありますか？（例: 絵文字禁止、HTMLタグ禁止、改行禁止 etc.）
[Answer]

---

## 2. UI/UX

### 2.1 入力 UI の配置

[Question] 備考入力欄は打刻ボタンの横に常時表示しますか？それともボタン押下後のダイアログ/モーダルで入力させますか？
[Answer]

### 2.2 プレースホルダー

[Question] 入力欄のプレースホルダーテキストは何にしますか？（例: 「遅刻理由、直行先など」「任意メモ」）
[Answer]

### 2.3 表示幅

[Question] 入力欄は1行（input）にしますか？複数行（textarea）にしますか？複数行の場合、初期表示行数は？
[Answer]

### 2.4 履歴画面での表示

[Question] 勤怠履歴画面で備考をどう表示しますか？（例: テーブル列に追加、ツールチップ、展開行）
[Answer]

---

## 3. データベース設計

### 3.1 カラム型と長さ

[Question] `attendance_records` テーブルに追加するカラムの型は何にしますか？（候補: `VARCHAR(200)` / `VARCHAR(500)` / `TEXT`）
[Answer]

### 3.2 NULL 許容

[Question] カラムは NULL 許容（備考なし = NULL）ですか？それとも空文字をデフォルトにしますか？
[Answer]

---

## 4. API 設計

### 4.1 リクエスト例

[Question] clock-in / clock-out の API リクエストにどう備考を含めますか？以下の形式案を確認してください:

```json
// 案A: クエリパラメータに追加
POST /api/attendance/clock-in?employeeId=xxx&memo=遅刻:電車遅延

// 案B: リクエストボディに変更
POST /api/attendance/clock-in
Content-Type: application/json
{
  "employeeId": "xxx",
  "memo": "遅刻:電車遅延"
}
```

どちらの方式を採用しますか？または別の方式がありますか？
[Answer]

### 4.2 レスポンス例

[Question] レスポンスに備考を含めますか？含める場合、以下の形式で問題ないですか？

```json
{
  "id": "xxx",
  "workDate": "2026-07-13",
  "clockIn": "2026-07-13T09:00:00Z",
  "clockOut": null,
  "memo": "遅刻:電車遅延",
  "corrected": false
}
```

[Answer]

### 4.3 エラーレスポンス

[Question] 備考バリデーションエラー時のレスポンスは以下で問題ないですか？

```json
// 文字数超過
{
  "status": 400,
  "title": "Bad Request",
  "detail": "備考は200文字以内で入力してください",
  "errors": { "memo": "200文字以内で入力してください" }
}
```

[Answer]

---

## 5. エッジケース

### 5.1 空白のみ

[Question] 空白文字のみの備考（例: "   "）は有効な入力として受け付けますか？それとも trim して空なら NULL にしますか？
[Answer]

### 5.2 既存レコードとの互換性

[Question] 既存の打刻レコード（備考なし）はどう扱いますか？マイグレーションで NULL を設定しますか？
[Answer]

### 5.3 勤怠修正との関連

[Question] 勤怠修正申請（correction）で備考も修正対象にしますか？
[Answer]

### 5.4 CSV/PDF レポート

[Question] 月次レポートの CSV/PDF 出力に備考を含めますか？含める場合のカラム位置は？
[Answer]

### 5.5 チーム/管理者画面

[Question] マネージャーのチーム勤怠画面や管理者の全社員画面で備考を表示しますか？
[Answer]

### 5.6 XSS/インジェクション

[Question] 備考に HTML タグや特殊文字が含まれた場合のサニタイズ方針は？（React のデフォルトエスケープで十分か、サーバー側でも除去するか）
[Answer]

### 5.7 長文入力時の UX

[Question] 文字数が上限に近づいた際にカウンター表示しますか？上限超過時のリアルタイムフィードバックは？
[Answer]

---

## 6. 受け入れ基準（Acceptance Criteria）

> 全 [Answer] が確定してから記入する。

- [ ] （回答確定後に記入）
- [ ] （回答確定後に記入）
- [ ] （回答確定後に記入）
