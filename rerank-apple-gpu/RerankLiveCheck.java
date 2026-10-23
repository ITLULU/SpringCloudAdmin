import java.util.ArrayList;
import java.util.List;

import com.example.ragkb.retrieval.RetrievedChunk;
import com.example.ragkb.retrieval.TeiReranker;

public class RerankLiveCheck {

    static String q() { return "向量检索里 RRF 融合的作用是什么"; }

    static List<String> texts() {
        return new ArrayList<>(List.of(
                "倒数排名融合把多路召回的名次而不是分数作为输入，避免不同评分体系不可比",
                "BM25 是词袋模型，靠词频与逆文档频率给关键词匹配打分",
                "RRF 的平滑常数 k 通常取 60，用来削弱头部名次的过度主导",
                "查询重写可以在召回前把口语化问题改写成检索友好的表达",
                "向量数据库的 HNSW 索引通过多层近邻图换取检索速度与精度的折中",
                "重排模型用 cross-encoder 让 query 与文档做 token 级 attend，因此比双塔更准但更贵",
                "分块策略影响召回质量：块太长会稀释语义，太短会丢上下文",
                "元数据过滤能在向量检索前先按租户或时间范围收窄候选集",
                "混合检索的失败模式之一是两路都命中同一批文档，融合后没有增量覆盖",
                "评测机用召回率、MRR 与 nDCG 三个指标分别看覆盖、排名与位置加权质量",
                "嵌入模型把文本映射为稠密向量，语义相近的文本在向量空间距离更近",
                "稀疏检索可解释性强，但对同义改写和无关键词重叠的查询无能为力",
                "top-k 召回过多会把噪声塞进上下文窗口，过少则直接漏掉正确答案",
                "重排的成本与候选数成正比，所以只在粗筛后的候选池上做精排",
                "多路召回互补：关键词路保证精确命中，向量路保证语义泛化",
                "相关性阈值截断有风险：分数不可跨模型比较，一刀切会误杀正确文档",
                "向量索引的召回率与查询延迟此消彼长，需要在两者之间折中",
                "文档切分要保留结构信息，标题与段落边界常常是关键信号",
                "日志与埋点可以帮助定位一次检索失败到底发生在召回还是精排",
                "增量索引让新文档在秒级可检，避免全量重建带来的停机",
                "缓存热点查询的召回结果可以显著降低尾延迟",
                "冷启动查询没有历史可依赖，只能靠模型本身的相关性判断",
                "同一条查询在不同模型版本上的分数不可直接比较",
                "评测集需要人工标注，标注一致性直接影响指标可信度",
                "A/B 实验能验证一次检索改动是否真的提升了线上点击率",
                "灰度发布可以控制检索改动的影响面，出问题能快速回滚",
                "查询理解模块负责纠错、扩词与意图识别",
                "倒排索引是稀疏检索的底层结构，词项到文档列表的映射",
                "语义去重可以避免候选池被近似重复的段落占满",
                "上下文压缩可以在有限窗口内塞进更多证据",
                "长上下文模型并不能自动替代检索，幻觉问题依然存在",
                "检索增强生成把外部知识注入提示词，降低模型编造答案的概率",
                "证据引用需要能回溯到原文位置，才能支撑可审计的回答",
                "权限过滤必须在检索阶段生效，否则越权内容会进上下文",
                "多租户隔离通常靠元数据过滤实现",
                "索引更新与查询并发时需要保证读到的是一致快照",
                "分片数影响检索并行度与聚合成本",
                "向量归一化后点积等价于余弦相似度",
                "近邻搜索的近似算法用精度换速度",
                "重排模型的训练依赖标注好的查询-文档对",
                "在线学习可以按点击反馈微调排序",
                "降级策略要覆盖依赖超时，避免整条链路被拖垮",
                "熔断器在下游持续失败时快速拒绝，给它恢复时间",
                "限流按调用方分桶可以防止单租户挤占容量",
                "可观测性靠指标、日志与链路追踪三件套",
                "告警要有明确阈值与升级路径，否则等于没有",
                "配置外置让容量参数改起来不用重建镜像",
                "密钥不应写进配置文件，走环境变量或秘密文件",
                "备份需要定期做恢复演练，未验证的备份不算备份",
                "灰度与回滚是上线流程里最小可用的安全网"));
    }

    static List<RetrievedChunk> candidates(int n) {
        List<String> ts = texts();
        List<RetrievedChunk> out = new ArrayList<>();
        for (int i = 0; i < n && i < ts.size(); i++) {
            double fused = 1.0 / (60 + i + 1);
            out.add(new RetrievedChunk("c" + i, ts.get(i), ts.get(i), "unit", 0.0, 0.0, fused, null));
        }
        return out;
    }

    static void run(String label, String url, int n, int topN) {
        TeiReranker r = new TeiReranker(url);
        List<RetrievedChunk> in = candidates(n);
        long t0 = System.nanoTime();
        List<RetrievedChunk> out = r.rerank(q(), in, topN);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        boolean scored = out.stream().allMatch(c -> c.rerankScore() != null);
        System.out.printf("%-22s n=%d out=%d %5dms 精排真实生效=%s%n", label, n, out.size(), ms, scored);
        if (scored) {
            for (RetrievedChunk c : out) {
                System.out.printf("    idx=%s score=%.6f  %s%n",
                        c.chunkId(), c.rerankScore(), c.content().substring(0, Math.min(24, c.content().length())));
            }
        }
    }

    public static void main(String[] args) {
        String gpu = "http://127.0.0.1:8089/rerank";
        run("Metal-20候选", gpu, 20, 5);
        run("Metal-40候选(真实top-n上限)", gpu, 40, 5);
        // 反向验证：端口换成死端口，必须走降级（rerankScore 全 null）而不是抛异常
        run("反向-死端口应降级", "http://127.0.0.1:8099/rerank", 20, 5);
    }
}
