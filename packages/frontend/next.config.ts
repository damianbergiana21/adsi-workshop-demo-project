import path from "node:path";
import type { NextConfig } from "next";

const isDev = process.env.NODE_ENV === "development";

// SageMaker Code Editor のプロキシ配下で動かすモード。
// next build + next start（NODE_ENV=production）で起動するが、
// static export は無効・/api の rewrites は有効にする必要があるため、
// NODE_ENV とは独立した専用フラグで分岐する。
const isSagemaker = process.env.SAGEMAKER === "1";

// プロキシ配下では HTML/JS が basePath で配信されるため、Next.js 側の
// basePath / assetPrefix をプロキシパス（例: /absports/3000）に一致させる。
const basePath = process.env.NEXT_PUBLIC_BASE_PATH;

// /api をバックエンド(localhost:8080)へ転送するのは dev か SAGEMAKER のとき。
// 本番デプロイ(static export)では転送せず、別途配信する。
const needsApiRewrite = isDev || isSagemaker;

// static export は本番デプロイ時のみ（dev / SAGEMAKER では使わない）。
const useStaticExport = !isDev && !isSagemaker;

const nextConfig: NextConfig = {
  ...(useStaticExport ? { output: "export" } : {}),
  ...(basePath ? { basePath, assetPrefix: basePath } : {}),
  // プロキシは末尾スラッシュの有無で挙動が変わるため、Next 側の
  // trailing-slash リダイレクトを抑止して二重リダイレクトを避ける。
  ...(isSagemaker ? { skipTrailingSlashRedirect: true } : {}),
  turbopack: {
    root: path.resolve(__dirname),
  },
  ...(needsApiRewrite
    ? {
        async rewrites() {
          return [
            {
              source: "/api/:path*",
              destination: "http://localhost:8080/api/:path*",
            },
          ];
        },
      }
    : {}),
};

export default nextConfig;
