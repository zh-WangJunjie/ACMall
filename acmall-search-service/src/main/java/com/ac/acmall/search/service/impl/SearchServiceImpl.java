package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    JestClient jestClient;

    @Override
    public List<PmsSearchSkuInfo> search(PmsSearchParam pmsSearchParam) {
        //这个方法就是利用elasticsearch搜索引擎去es中搜索结果
        //PmsSearchParam包含3个参数：catalog3Id、keyword、valueId(平台属性)
        //当然，要想查到结果，es必须要有关于PmsSearchSkuInfo的内容
        //首先，要根据pmsSearchParam生成一个query sql语句，这个query是es中的查询语句
        String query = getMyQuery(pmsSearchParam);
        //可以在控制台打印一下这个query
        System.out.println(query);

        //声明一个PmsSearchSkuInfo集合容器
        List<PmsSearchSkuInfo> searchSkuInfoList = new ArrayList<>();

        //声明一个Search
        Search search = new Search.Builder(query).addIndex("gmall0722").addType("PmsSearchSkuInfo").build();

        try {
            //执行搜索并接收返回结果
            SearchResult searchResult = jestClient.execute(search);
            //解析搜索结果，并转换为PmsSearchSkuInfo的集合进行返回
            //先判断搜索结果是否为空
            if (searchResult != null) {
                //提取hits，需要把类型sourceType传过去
                List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = searchResult.getHits(PmsSearchSkuInfo.class);
                //解析hits，先判断hits是否为空
                if (hits != null) {
                    for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
                        //解析hit，将hit转换成PmsSearchSkuInfo
                        PmsSearchSkuInfo pmsSearchSkuInfo = hit.source;

                        //搜索结果keyword高亮
                        Map<String, List<String>> highlight = hit.highlight;
                        if (highlight != null) {
                            String highlightName = highlight.get("skuName").get(0);
                            pmsSearchSkuInfo.setSkuName(highlightName);
                        }

                        //将PmsSearchSkuInfo存入list容器
                        searchSkuInfoList.add(pmsSearchSkuInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return searchSkuInfoList;
    }

    /**
     * 本方法用于将pmsSearchParam生成一个query语句
     * */
    public String getMyQuery(PmsSearchParam pmsSearchParam){
        //提取pmsSearchParam中的属性值
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String keyword = pmsSearchParam.getKeyword();
        String[] valueIds = pmsSearchParam.getValueId();
        //声明一个SearchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //声明一个BoolQueryBuilder
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //过滤用TermsQueryBuilder
        //搜索用MatchQueryBuilder
        //catalog3Id属于过滤条件，先判断catalog3Id是否为空
        if (!StringUtils.isEmpty(catalog3Id)){
            //过滤用TermsQueryBuilder，声明一个TermsQueryBuilder name是"catalog3Id"  value是catalog3Id
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",catalog3Id);
            //调用过滤器
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //keyword属于搜索条件，先判断keyword是否为空
        if (!StringUtils.isEmpty(keyword)){
            //搜索用（匹配）MatchQueryBuilder，声明一个MatchQueryBuilder name是"skuName"  text是keyword
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",keyword);
            //调用must
            boolQueryBuilder.must(matchQueryBuilder);

            //搜索结果keyword高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red;font-weight:bolder;'>");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlight(highlightBuilder);
        }

        //valueIds属于过滤条件，先判断valueIds是否为空
        if (valueIds!=null&&valueIds.length>0){
            //valueIds在es中对应的是skuAttrValueList中的valueId
            //先遍历，再挨个儿过滤
            for (String valueId : valueIds) {
                //过滤用TermsQueryBuilder，声明一个TermsQueryBuilder name是"skuAttrValueList.valueId" value是valueId
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                //调用过滤器
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //调用query
        searchSourceBuilder.query(boolQueryBuilder);
        //分页，将数据全部显示
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(200);

        //sql生成后，将它转换成字符串返回
        String query = searchSourceBuilder.toString();

        return query;
    }
}
