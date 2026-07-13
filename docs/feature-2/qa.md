# Feature: 有給休暇申請機能 — 仕様 Q&A

> Issue: #6
> ステータス: 回答待ち（全 [Answer] が埋まったら Issue に「Answers completed」とコメントしてください）

---

## 1. 申請フロー

### 1.1 申請対象日

[Question] 有給休暇は当日・過去日・未来日のどれを申請対象としますか？（例: 未来日のみ、当日以降、過去日も可）
[Answer]

### 1.2 申請単位

[Question] 申請単位は「1日単位」のみですか？半日（午前休/午後休）や時間単位の有給もサポートしますか？
[Answer]

### 1.3 連続日数

[Question] 複数日にまたがる申請は1件の申請としてまとめますか？それとも1日1件で個別に申請しますか？
[Answer]

### 1.4 申請理由

[Question] 申請時に理由の記入は必須ですか？任意ですか？文字数制限は？
[Answer]

### 1.5 申請キャンセル

[Question] 申請者は承認前の申請をキャンセルできますか？承認後のキャンセルは？
[Answer]

---

## 2. 承認フロー

### 2.1 承認者

[Question] 承認者は申請者の所属部署のマネージャーですか？それとも特定のロール（ADMIN）ですか？複数段階承認はありますか？
[Answer]

### 2.2 却下理由

[Question] 却下時に理由の記入は必須ですか？
[Answer]

### 2.3 承認期限

[Question] 承認に期限はありますか？（例: 申請から3営業日以内に承認/却下しない場合は自動承認 or リマインド通知）
[Answer]

### 2.4 承認通知

[Question] 承認/却下時に申請者に通知しますか？通知方法は？（例: アプリ内通知、メール、なし）
[Answer]

---

## 3. 有給残日数

### 3.1 初期付与

[Question] 有給の初期付与日数は何日ですか？付与タイミングは？（例: 入社日に10日付与、毎年4月1日に付与）
[Answer]

### 3.2 残日数管理

[Question] 残日数はシステムで自動管理しますか？それとも手動（管理者が設定）ですか？
[Answer]

### 3.3 残日数不足

[Question] 残日数が0の場合に申請は拒否されますか？それとも「残日数不足」の警告付きで申請可能ですか？
[Answer]

### 3.4 繰越

[Question] 未使用の有給は翌年に繰り越せますか？繰越上限は？
[Answer]

---

## 4. 勤怠記録への反映

### 4.1 反映タイミング

[Question] 承認された有給はいつ勤怠記録に反映されますか？（例: 承認時に即時反映、対象日当日に自動反映）
[Answer]

### 4.2 反映方法

[Question] 有給日は `attendance_records` に特別なレコードとして作成しますか？それとも別テーブル（例: `leave_records`）で管理しますか？
[Answer]

### 4.3 月次レポート

[Question] 月次レポートでの有給日の表示方法は？（例: 「有給」と表示、勤務時間は0 or 8h みなし）
[Answer]

### 4.4 勤務日数カウント

[Question] 有給取得日は「勤務日数」にカウントしますか？「欠勤日数」からは除外しますか？
[Answer]

---

## 5. UI/UX

### 5.1 申請画面

[Question] 申請画面はどこに配置しますか？（例: サイドバーに新規メニュー「有給申請」追加、ダッシュボードにボタン追加）
[Answer]

### 5.2 カレンダー表示

[Question] 申請時にカレンダーUIで日付を選択させますか？テキスト入力ですか？
[Answer]

### 5.3 残日数表示

[Question] 残日数は常時表示しますか？（例: サイドバー、ダッシュボード、申請画面のみ）
[Answer]

### 5.4 承認画面

[Question] マネージャーの承認画面は既存の「承認一覧（/approvals）」に統合しますか？それとも別ページにしますか？
[Answer]

### 5.5 履歴表示

[Question] 社員は自分の有給申請履歴を一覧で確認できますか？フィルタ項目は？（ステータス、期間など）
[Answer]

---

## 6. データベース設計

### 6.1 テーブル構成

[Question] 有給申請を管理するテーブル構成はどうしますか？以下の案を確認してください:

```sql
-- 案A: 単独テーブル
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    leave_date DATE NOT NULL,
    leave_type VARCHAR(20) NOT NULL,  -- FULL_DAY, AM_HALF, PM_HALF
    reason VARCHAR(200),
    status VARCHAR(20) NOT NULL,       -- PENDING, APPROVED, REJECTED, CANCELLED
    approver_id UUID REFERENCES employees(id),
    reject_reason VARCHAR(200),
    approved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 案B: 申請テーブル + 有給残高テーブル
-- leave_requests + leave_balances
```

どの構成を採用しますか？
[Answer]

### 6.2 有給残高テーブル

[Question] 有給残日数を管理するテーブルは必要ですか？必要な場合の構成は？
[Answer]

---

## 7. API 設計

### 7.1 申請 API

[Question] 有給申請の API エンドポイントとリクエスト形式は以下で問題ないですか？

```json
POST /api/leave-requests
{
  "leaveDate": "2026-08-01",
  "leaveType": "FULL_DAY",
  "reason": "家族旅行"
}
```

[Answer]

### 7.2 承認/却下 API

[Question] 承認・却下の API 形式は以下で問題ないですか？

```json
PATCH /api/leave-requests/{id}/approve

PATCH /api/leave-requests/{id}/reject
{
  "rejectReason": "繁忙期のため"
}
```

[Answer]

### 7.3 一覧取得 API

[Question] 一覧取得のフィルタパラメータは何が必要ですか？（例: status, month, employeeId）

```
GET /api/leave-requests?status=PENDING&month=2026-08
GET /api/leave-requests/pending?managerId=xxx  (マネージャー向け)
```

[Answer]

### 7.4 残日数取得 API

[Question] 残日数を取得する API は必要ですか？形式は？

```
GET /api/leave-balance?employeeId=xxx
→ { "totalDays": 20, "usedDays": 5, "remainingDays": 15 }
```

[Answer]

---

## 8. エッジケース

### 8.1 重複申請

[Question] 同じ日に対して複数の有給申請を出せますか？出せない場合のバリデーションは？
[Answer]

### 8.2 勤怠記録との競合

[Question] 既に出勤打刻がある日に有給申請が承認された場合、どう処理しますか？
[Answer]

### 8.3 退職者

[Question] 退職日以降の有給申請は拒否しますか？退職予定者の残日数消化はどう扱いますか？
[Answer]

### 8.4 祝日・休日

[Question] 土日祝日に対する有給申請は拒否しますか？祝日マスタは必要ですか？
[Answer]

### 8.5 承認者不在

[Question] マネージャーが退職・異動した場合、未承認の申請はどうなりますか？
[Answer]

---

## 9. 受け入れ基準（Acceptance Criteria）

> 全 [Answer] が確定してから記入する。

- [ ] （回答確定後に記入）
- [ ] （回答確定後に記入）
- [ ] （回答確定後に記入）
