# 基于官方 Elasticsearch 8.13.4 镜像，安装 IK 中文分词插件
FROM docker.elastic.co/elasticsearch/elasticsearch:8.13.4

# 安装 analysis-ik 插件（版本必须与 ES 版本一致）
# 使用 infinilabs 国内源加速下载；如网络正常也可改用官方 GitHub release 地址
RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.13.4
