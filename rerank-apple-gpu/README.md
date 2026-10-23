# 在 Mac 的 GPU 上跑精排（Apple Metal + llama.cpp）

结论先说：**能跑，而且比现在本机的 CPU 方案快一个数量级**——但不是用 Docker 的 GPU 镜像，
那条路在 macOS 上根本不存在。本目录是实测过的落地方案，数字都是本机 M1 Pro 上跑出来的。

## 一、为什么 `rerank-gpu/` 那套在这台机器上不可能生效

三层证据，都不是推测：

1. **镜像本身只有 x86+CUDA**。`text-embeddings-inference:1.9` 是 linux/amd64 的 CUDA 构建，
   里面链接的是 `libcuda.so`；Apple Silicon 上既没有 CUDA 运行时，也没有 amd64 原生内核。
2. **Docker Desktop for Mac 没有 GPU 直通**。`docker info` 里 runtime 只有 `io.containerd.runc.v2`
   与 Docker Desktop 自己的 runtime，**没有 `nvidia`**；`rerank-gpu/docker-compose.yml:22` 那句
   `devices: driver: nvidia` 在本机直接报 `could not find the device nvidia`。
   NVIDIA 的容器运行时（nvidia-container-toolkit）只发布了 Linux 与 Windows/WSL2(GPU-PV)。
3. **CUDA 不支持 macOS**。NVIDIA 在 CUDA 10.2 之后就停止支持 macOS；Apple GPU 走的是 **Metal**，
   与 CUDA 不是同一套编程模型。所以"H100-MacBook"那种说法（`gpu-server.md` 里记的那次评审）
   是**产品口径错误**，不是可以买到的东西——真要 NVIDIA GPU 就得换 Linux 主机或 WSL2 台式机。

> 一句话：Mac 上用 GPU 的正确姿势不是"让 Docker 看见 GPU"，而是**换一个会说 Metal 的推理引擎**。

## 二、方案：llama.cpp 的 `--reranking`（原生 Metal）

llama.cpp 自带 Metal 后端，且它的 `/rerank` 端点**与 TEI 契约一致**，这是本方案能零改动接入的关键：

| 项 | TEI（现在的 CPU 方案） | llama-server（本方案） |
|---|---|---|
| 请求 | `POST /rerank {"query","texts"}` | 同 |
| 响应 | 裸数组 `[{"index","score"}]` | 同 |
| 别名 | `/reranking` | `/rerank`、`/reranking`、`/v1/rerank` |
| 分数口径 | 已过 sigmoid，0~1 | **未过 sigmoid 的 logit，可为负** |
| GPU | 无（CPU/Rosetta） | Metal（`-ngl 99` 全层上 GPU） |

**分数口径这条不是细节**：`rerankScore` 在 rag-kb 里只用于排序与调试展示，没有任何 0~1 阈值判断
（`RetrievedChunk.withRerank` → `TeiReranker` 按分数降序截断），所以负分不会出事；
但**跨后端比较分数值没有意义，只能比名次**。

### 用法

```bash
bash install.sh          # 下载 llama.cpp macOS arm64 预编译包 + bge-reranker GGUF，并核 SHA256
bash start.sh            # 127.0.0.1:8089/rerank，pid 落在 llama.pid
bash bench.sh            # 与 CPU 端点同题对照（耗时 + 名次一致性）
bash stop.sh             # TERM → 15s → KILL，并清 pid 文件
```

可调环境变量：`QUANT`（Q4_K_M 默认 / Q5_K_M / Q8_0）、`CTX`、`NGL`、`PORT`、`ROUNDS`、`DOCS`。

`install.sh` 不会带着可疑文件继续走：包必须先过 `tar -tzf` 与"含 `libggml-metal`"两道检查，
模型必须与模型仓库自带的 `SHA256SUMS` 逐字节对上（期望值现取现比，不手抄散列；
那个文件是 CRLF 换行，脚本里专门 `tr -d '\r'`，否则会把好文件判成"查无此项"）。

## 三、实测数据（本机 Apple M1 Pro，20 条候选，warmup 校验通过后再取 3 轮）

| 端点 | 配置 | 平均耗时 | 相对 Metal |
|---|---|---|---|
| `127.0.0.1:8089` | llama.cpp `-ngl 99`（Metal 全层） | **296 ms** | 1.0× |
| `127.0.0.1:8091` | 同一二进制 `-ngl 0`（纯 CPU） | 785 ms | 慢 2.65× |
| `127.0.0.1:8088` | TEI CPU（amd64 + Rosetta 转译） | 4792 ms | 慢 16.2× |

- **Metal 真的在用**，不是"配置里写了但跑在 CPU 上"：`llama-bench` 启动日志出现
  `ggml_metal_device_init` / `ggml_metal_library_init: using embedded metal library`，
  并现场编译了 `fa_q4_0`…`fa_q8_0` 等 Metal kernel（只有真正实例化 Metal 后端才会编译这些）；
  `llama-server --list-devices` 枚举到 `MTL0: Apple M1 Pro (21845 MiB)`。
  并且 `-ngl 99` 比 `-ngl 0` 快 2.65× —— 这是**行为层面**的证据，比看日志硬。
- **排序一致性**：Metal 与 TEI 的 Top-5 **集合完全相同**（`{2,0,8,7,4}` vs `{2,8,0,7,4}`），Top-1 一致；
  20 条候选的名次绝对差之和 14，平均每条 0.7 位。差异都发生在近似并列的名次上
  （Metal 侧第 2/3 名分差仅 0.078 logit）。同一二进制换设备也是同样现象（`-ngl 99` vs `-ngl 0`
  最大分差 0.515，且只出现在无关填充文档上）。**量化噪声级，不是语义分歧。**
- 40 条候选（真实 `top-n=20` 融合后的最大发送量）：TEI CPU 约 9.4 s，Metal 已补测 = 653 ms 首测 / 449 ms 复测
  （Java `RestClient` 侧口径，含序列化与一次连接建立，比 `bench.sh` 的 curl 口径略高）。
  `-np 4` 只影响**并发**请求排队，单请求内的 40 条是整批过的，所以这里没有排队。

## 四、接入 rag-kb：改一个配置值，Java 代码零改动

```bash
export RERANK_ENABLED=true
export RERANK_URL=http://127.0.0.1:8089/rerank
```

`TeiReranker` 已经同时认"裸数组 + `score`"和"对象 + `relevance_score`"两种形态
（`TeiRerankerTest#teiNativeBareArray_isParsedWithScoreField` 钉的就是前者），
llama-server 回的正是裸数组 `{"index","score"}`。

> 这一条接入结论是 **`[动态]`**（2026-10-23 22:50 实测，用 `TeiReranker` **生产代码本体**直接打 8089，零改动）：
>
> ```bash
> # 依赖仓在 /Users/a/m2/repository（settings.xml 里配的），联网走 aliyun 镜像
> export JAVA_HOME=/Users/a/tools/jdk-21.0.12.1+1/Contents/Home
> cd /Users/a/source/spring-ai-agent && mvn -B test     # 40 测试 0 失败，BUILD SUCCESS
> mvn -B -q -DskipTests -pl rag-kb dependency:build-classpath -Dmdep.outputFile=/tmp/ragkb_cp.txt
> CP="/Users/a/source/spring-ai-agent/rag-kb/target/classes:$(cat /tmp/ragkb_cp.txt)"
> javac -nowarn -cp "$CP" -d /tmp/rrcheck /Users/a/source/SpringCloudAdmin/rerank-apple-gpu/RerankLiveCheck.java
> java -cp "$CP:/tmp/rrcheck" RerankLiveCheck           # 从 /tmp 跑：这台机器某些 cwd 下出站连接会被拦
> ```
>
> 实测三件事，缺一不可：
> 1. **20 候选 445 ms、40 候选 653 ms 均整批返回**，Top-1 = `RRF 的平滑常数 k…`（这题的正确答案）；
> 2. **`rerankScore` 非 null** ⇒ 走的是真精排分支，不是"融合名次截断"的静默降级（这条判据能区分二者：降级路径返回的 record 没打过分）；
> 3. **反向验证**：把 URL 换成死端口 8099 ⇒ WARN 留痕 + 18 ms 降级、不抛异常。只验正常路径不算验过。
>
> 分数是负数（llama.cpp 回原始 logit，TEI 回 sigmoid 后的 0~1），`parseAndApply` 与 `withRerank`
> 实测照收不误——再次确认全链路只做排序、没有阈值判断，所以负分不会把候选全砍掉。
> 40 候选时 653 ms 仍远低于 `SimpleClientHttpRequestFactory` 的 5 s 读超时。

回滚：`RERANK_URL` 改回 `http://127.0.0.1:8088/rerank` 即可，`bash stop.sh` 停掉 Metal 服务。

## 五、顺带查出并修掉的两个本地配置缺陷（它们让"精排"一直是假的）

1. **`--max-client-batch-size 8` 与调用方形状冲突**。`TeiReranker.rerank` 把融合后的**全部候选**
   一次发过去（`rag.retrieval.top-n=20` ⇒ 可达 40 条），TEI 直接 400 `batch size N > maximum allowed batch size 8`；
   Java 侧按设计降级成"融合名次截断"，于是 `enabled=true` 也等于没开，只留一行 WARN。
   → 已抬到 48。
2. **`--max-concurrent-requests` 其实是信号量池大小**，不只管并发：TEI 按**每条待打分文本**占 1 个 permit
   （`core/src/infer.rs` `try_acquire_permit` → `no permits available` → `Model is overloaded`）。
   抬完 batch 后 20 条仍被拒，实测断点正好在 permit 数（16）上。→ 已抬到 64，
   复验 20/40 条均整批返回，容器 `OOM=false`、`restarts=0`、内存 2.65 GiB。

真正兜住内存的一直是 `--max-batch-tokens 1024`（TEI 会把客户端 batch 再切成 ≤1024 token 的内部批），
不是那两个客户端参数——所以这次放宽没有把 OOM 防线撤掉，也是当初为躲 OOM 把 batch 掐到 8 的直觉管错了对象。

`bench.sh` 也补了响应校验：加校验前的第一轮跑分给出过 "CPU 60 ms 比 GPU 190 ms 快" 的结论，
那是**拒答只要 60 ms** 的假绿灯。现在 warmup 与每轮都必须"条数对得上且每条有数值 score"才计入统计。

## 六、还没做 / 注意事项

- **没有常驻托管**：`start.sh` 是前台拉起 + pid 文件形态，重启机器后要手动再跑一次。
  要做 launchd 常驻可以加，但那是另一件事，别顺手塞进来。
- **端口 8089 无鉴权**，且 `install.sh` 起的 `llama-server` 只在 `127.0.0.1` 上监听；
  如果要给别的机器用，必须先加 API key（日志里那条 `security: no API key is set` WARN 就是它）。
- Q4_K_M 是本机自测档；要更保真换 `QUANT=Q8_0`（606 MB，下载与内存都翻倍）。
  换档后**必须重跑 `bench.sh`** 再决定，别把 Q4 的耗时当 Q8 的用。
- 这台机器的数字口径：单发串行、20 条候选、warmup 后 3 轮平均。不是压测，不含排队与并发。
