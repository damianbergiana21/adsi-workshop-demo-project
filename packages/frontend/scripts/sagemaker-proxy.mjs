// SageMaker Code Editor プレビュー用の極小リバースプロキシ。
//
// 経路:
//   ブラウザ  https://<domain>/codeeditor/default/absports/3000/...
//   └ ゲートウェイ → code-server(8888) が base-path "/codeeditor/default" を剥がす
//     └ absports は剥がさず転送 → このプロキシ(3000)に "/absports/3000/..." が届く
//       └ "/codeeditor/default" を前置して next(3001) へ転送
//
// next の basePath はフルパス "/codeeditor/default/absports/3000" にしてあるため、
// next が返すリダイレクト Location・アセット URL・fetch 先はすべて
// "/codeeditor/default/absports/3000/..." となり、ブラウザから見て正しいパスになる。
// absports は応答 Location を書き換えないので、前置の復元はこのプロキシの「受信時」だけでよい。

import http from "node:http";

const LISTEN_PORT = Number(process.env.PROXY_PORT ?? 3000);
const UPSTREAM_PORT = Number(process.env.UPSTREAM_PORT ?? 3001);
const PREFIX = process.env.PROXY_PREFIX ?? "/codeeditor/default";

const server = http.createServer((req, res) => {
  // code-server が剥がした PREFIX を復元して next へ渡す。
  const upstreamPath = req.url.startsWith(PREFIX) ? req.url : PREFIX + req.url;

  const proxyReq = http.request(
    {
      host: "127.0.0.1",
      port: UPSTREAM_PORT,
      method: req.method,
      path: upstreamPath,
      headers: req.headers,
    },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers);
      proxyRes.pipe(res);
    },
  );

  proxyReq.on("error", (err) => {
    res.writeHead(502, { "content-type": "text/plain; charset=utf-8" });
    res.end(`proxy error: ${err.message}`);
  });

  req.pipe(proxyReq);
});

server.listen(LISTEN_PORT, "0.0.0.0", () => {
  console.log(
    `sagemaker-proxy: :${LISTEN_PORT} → 127.0.0.1:${UPSTREAM_PORT} (prefix "${PREFIX}")`,
  );
});
