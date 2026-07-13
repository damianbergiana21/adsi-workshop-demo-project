# Feature: 有給休暇申請機能 — 仕様 Q&A

> Issue: #6
> ステータス: 回答待ち（全 [Answer] が埋まったら Issue に「Answers completed」とコメントしてください）

---

## 1. 申請フロー

### 1.1 申請対象日

[Question] 有給休暇は当日・過去日・未来日のどれを申請対象としますか？（例: 未来日のみ、当日以降、過去日も可）
[Answer] 未来日のみを申請対象とします（当日申請は緊急時のみ例外的に管理者経由で対応）。

### 1.2 申請単位

[Question] 申請単位は「1日単位」のみですか？半日（午前休/午後休）や時間単位の有給もサポートしますか？
[Answer] 1日単位に加え、半日（午前休/午後休）もサポートします。時間単位はスコープ外とします。

### 1.3 連続日数

[Question] 複数日にまたがる申請は1件の申請としてまとめますか？それとも1日1件で個別に申請しますか？
[Answer] 連続する複数日は1件の申請としてまとめて申請できるようにします（開始日・終了日を指定）。

### 1.4 申請理由

[Question] 申請時に理由の記入は必須ですか？任意ですか？文字数制限は？
[Answer] 任意項目とし、最大200文字とします。

### 1.5 申請キャンセル

[Question] 申請者は承認前の申請をキャンセルできますか？承認後のキャンセルは？
[Answer] 承認前（PENDING状態）は申請者が自由にキャンセル可能です。承認後のキャンセルは、取消申請を新たに作成し、承認者の再承認を必要とします。

---

## 2. 承認フロー

### 2.1 承認者

[Question] 承認者は申請者の所属部署のマネージャーですか？それとも特定のロール（ADMIN）ですか？複数段階承認はありますか？
[Answer] 申請者の直属マネージャーを承認者とします。複数段階承認は導入せず、単一段階とします。

### 2.2 却下理由

[Question] 却下時に理由の記入は必須ですか？
[Answer] 必須とします。

### 2.3 承認期限

[Question] 承認に期限はありますか？（例: 申請から3営業日以内に承認/却下しない場合は自動承認 or リマインド通知）
[Answer] 自動承認は行いません。申請から3営業日以内に処理がない場合、承認者にリマインド通知を送信します。

### 2.4 承認通知

[Question] 承認/却下時に申請者に通知しますか？通知方法は？（例: アプリ内通知、メール、なし）
[Answer] 通知します。アプリ内通知とメールの両方で通知します。

---

## 3. 有給残日数

### 3.1 初期付与

[Question] 有給の初期付与日数は何日ですか？付与タイミングは？（例: 入社日に10日付与、毎年4月1日に付与）
[Answer] 労働基準法の基準に準拠します。入社日から6ヶ月経過時点で10日を付与し、以降は毎年4月1日を基準日として勤続年数に応じた日数を付与します。

### 3.2 残日数管理

[Question] 残日数はシステムで自動管理しますか？それとも手動（管理者が設定）ですか？
[Answer] システムで自動管理します（付与・消化・繰越をすべて自動計算）。

### 3.3 残日数不足

[Question] 残日数が0の場合に申請は拒否されますか？それとも「残日数不足」の警告付きで申請可能ですか？
[Answer] 残日数が不足する申請は拒否します（申請時点でバリデーションエラーとする）。

### 3.4 繰越

[Question] 未使用の有給は翌年に繰り越せますか？繰越上限は？
[Answer] 繰越可能とします。労働基準法に準拠し、繰越は翌年度分までとし、付与から2年で時効消滅とします。

---

## 4. 勤怠記録への反映

### 4.1 反映タイミング

[Question] 承認された有給はいつ勤怠記録に反映されますか？（例: 承認時に即時反映、対象日当日に自動反映）
[Answer] 承認時に即時反映します。

### 4.2 反映方法

[Question] 有給日は `attendance_records` に特別なレコードとして作成しますか？それとも別テーブル（例: `leave_records`）で管理しますか？
[Answer] `attendance_records` に種別（type = LEAVE）を持つ特別なレコードとして作成します。申請自体の管理は別テーブル（leave_requests）で行い、承認時に紐づくattendance_recordsレコードを生成します。

### 4.3 月次レポート

[Question] 月次レポートでの有給日の表示方法は？（例: 「有給」と表示、勤務時間は0 or 8h みなし）
[Answer] 「有給」と表示し、勤務時間は8h（所定労働時間）みなしとします。半日の場合は4hみなしとします。

### 4.4 勤務日数カウント

[Question] 有給取得日は「勤務日数」にカウントしますか？「欠勤日数」からは除外しますか？
[Answer] 勤務日数にカウントし、欠勤日数には含めません。

---

## 5. UI/UX

### 5.1 申請画面

[Question] 申請画面はどこに配置しますか？（例: サイドバーに新規メニュー「有給申請」追加、ダッシュボードにボタン追加）
[Answer] サイドバーに新規メニュー「有給申請」を追加します。

### 5.2 カレンダー表示

[Question] 申請時にカレンダーUIで日付を選択させますか？テキスト入力ですか？
[Answer] カレンダーUIで日付（開始日・終了日）を選択させます。

### 5.3 残日数表示

[Question] 残日数は常時表示しますか？（例: サイドバー、ダッシュボード、申請画面のみ）
[Answer] サイドバーに常時表示します。

### 5.4 承認画面

[Question] マネージャーの承認画面は既存の「承認一覧（/approvals）」に統合しますか？それとも別ページにしますか？
[Answer] 既存の承認一覧（/approvals）に統合します（種別フィルタで有給申請を絞り込み可能にします）。

### 5.5 履歴表示

[Question] 社員は自分の有給申請履歴を一覧で確認できますか？フィルタ項目は？（ステータス、期間など）
[Answer] 一覧で確認できるようにします。フィルタ項目はステータス（PENDING/APPROVED/REJECTED/CANCELLED）と期間（年度）とします。

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
[Answer] 案B（leave_requests + leave_balances）を採用します。残日数の自動管理・繰越計算を正確に行うため、残高テーブルを分離します。なお、複数日にまたがる申請（1.3）に対応するため、leave_requestsはleave_dateではなくstart_date/end_dateを持つ構成に変更します。

### 6.2 有給残高テーブル

[Question] 有給残日数を管理するテーブルは必要ですか？必要な場合の構成は？
[Answer] 必要です。年度ごとに管理する構成とします。

```sql
CREATE TABLE leave_balances (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    fiscal_year INT NOT NULL,
    granted_days DECIMAL(4,1) NOT NULL,
    carried_over_days DECIMAL(4,1) NOT NULL DEFAULT 0,
    used_days DECIMAL(4,1) NOT NULL DEFAULT 0,
    expires_at DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (employee_id, fiscal_year)
);
```

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

[Answer] 基本形式は問題ありませんが、連続日数申請（1.3）に対応するため、leaveDateをstartDate/endDateに変更します。

```json
POST /api/leave-requests
{
  "startDate": "2026-08-01",
  "endDate": "2026-08-03",
  "leaveType": "FULL_DAY",
  "reason": "家族旅行"
}
```

### 7.2 承認/却下 API

[Question] 承認・却下の API 形式は以下で問題ないですか？

```json
PATCH /api/leave-requests/{id}/approve

PATCH /api/leave-requests/{id}/reject
{
  "rejectReason": "繁忙期のため"
}
```

[Answer] 提示された形式で問題ありません。

### 7.3 一覧取得 API

[Question] 一覧取得のフィルタパラメータは何が必要ですか？（例: status, month, employeeId）

```
GET /api/leave-requests?status=PENDING&month=2026-08
GET /api/leave-requests/pending?managerId=xxx  (マネージャー向け)
```

[Answer] status, month（または fiscalYear）, employeeId の3つを必要なフィルタパラメータとします。マネージャー向けの `/pending?managerId=xxx` も提示の通り採用します。

### 7.4 残日数取得 API

[Question] 残日数を取得する API は必要ですか？形式は？

```
GET /api/leave-balance?employeeId=xxx
→ { "totalDays": 20, "usedDays": 5, "remainingDays": 15 }
```

[Answer] 必要です。提示された形式で問題ありません。

---

## 8. エッジケース

### 8.1 重複申請

[Question] 同じ日に対して複数の有給申請を出せますか？出せない場合のバリデーションは？
[Answer] 出せません。既存のPENDINGまたはAPPROVED状態の申請と期間が重複する場合、申請時にバリデーションエラーとします。

### 8.2 勤怠記録との競合

[Question] 既に出勤打刻がある日に有給申請が承認された場合、どう処理しますか？
[Answer] 有給申請を優先し、既存の打刻データは無効化した上で管理者に確認アラートを送信します（自動削除はせず、確認フローを挟みます）。

### 8.3 退職者

[Question] 退職日以降の有給申請は拒否しますか？退職予定者の残日数消化はどう扱いますか？
[Answer] 退職日以降の申請は拒否します。退職予定者の残日数消化は、退職前に本人・マネージャーが事前調整して申請する運用とし、システムでの自動買い取り処理は行いません。

### 8.4 祝日・休日

[Question] 土日祝日に対する有給休暇申請は拒否しますか？祝日マスタは必要ですか？
[Answer] 土日祝日は元々労働日ではないため申請を拒否します。祝日マスタが必要です（日本の祝日を管理するテーブルを別途用意します）。

### 8.5 承認者不在

[Question] マネージャーが退職・異動した場合、未承認の申請はどうなりますか？
[Answer] 未承認の申請は、後任マネージャーまたは上位管理者に自動的にエスカレーション（承認者を再割り当て）します。

---

## 9. 受け入れ基準（Acceptance Criteria）

- [ ] 未来日を対象に、1日単位または半日単位（AM/PM）で有給申請ができ、連続日数はstartDate/endDateで1件にまとめられる
- [ ] 残日数が不足する申請は拒否され、承認済みの有給は承認時に即座にattendance_recordsへ反映される
- [ ] 直属マネージャーが承認/却下でき、却下時は理由が必須、3営業日未処理でリマインド通知が送られる
- [ ] 有給の付与・消化・繰越（翌年度まで、2年で時効消滅）がleave_balancesにより自動管理される
- [ ] 同一期間の重複申請、退職日以降の申請、土日祝日への申請がそれぞれ適切にバリデーションで拒否される
- [ ] 承認者が不在（退職・異動）になった場合、未承認申請が後任者に自動エスカレーションされる
