package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SpuServiceImpl implements SpuService {
    @Autowired
    private PmsProductInfoMapper pmsProductInfoMapper;
    @Autowired
    private PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;
    @Autowired
    private PmsProductSaleAttrMapper pmsProductSaleAttrMapper;
    @Autowired
    private PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;
    @Autowired
    private PmsProductImageMapper pmsProductImageMapper;
    @Autowired
    private PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    private PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    private PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    private PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    private SpuSaleAttrListCheckBySkuMapper spuSaleAttrListCheckBySkuMapper;
    @Autowired
    private SkuSaleAttrValueListBySpuMapper skuSaleAttrValueListBySpuMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {
        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> pmsProductInfoList = pmsProductInfoMapper.select(pmsProductInfo);
        //spuSaleAttrList  spuImageList
        for (PmsProductInfo productInfo : pmsProductInfoList) {
            String id = productInfo.getId();
            PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
            pmsProductSaleAttr.setProductId(id);
            List<PmsProductSaleAttr> productSaleAttrList = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
            productInfo.setSpuSaleAttrList(productSaleAttrList);

            PmsProductImage pmsProductImage = new PmsProductImage();
            pmsProductImage.setProductId(id);
            List<PmsProductImage> productImageList = pmsProductImageMapper.select(pmsProductImage);
            productInfo.setSpuImageList(productImageList);
        }
        return pmsProductInfoList;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        List<PmsBaseSaleAttr> pmsBaseSaleAttrList = pmsBaseSaleAttrMapper.selectAll();
        return pmsBaseSaleAttrList;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrList = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);

        for (PmsProductSaleAttr productSaleAttr : pmsProductSaleAttrList) {
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            String saleAttrId = productSaleAttr.getSaleAttrId();
            pmsProductSaleAttrValue.setSaleAttrId(saleAttrId);
            pmsProductSaleAttrValue.setProductId(spuId);
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValueList = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            productSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValueList);
        }
        return pmsProductSaleAttrList;
    }

    @Override
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {
        pmsProductInfoMapper.insertSelective(pmsProductInfo);

        String spuId = pmsProductInfo.getId();
        //添加销售属性
        List<PmsProductSaleAttr> spuSaleAttrList = pmsProductInfo.getSpuSaleAttrList();
        for (PmsProductSaleAttr pmsProductSaleAttr : spuSaleAttrList) {
            pmsProductSaleAttr.setProductId(spuId);
            pmsProductSaleAttrMapper.insertSelective(pmsProductSaleAttr);
            //添加销售属性值
            List<PmsProductSaleAttrValue> spuSaleAttrValueList = pmsProductSaleAttr.getSpuSaleAttrValueList();
            for (PmsProductSaleAttrValue pmsProductSaleAttrValue : spuSaleAttrValueList) {
                pmsProductSaleAttrValue.setProductId(spuId);
                pmsProductSaleAttrValueMapper.insertSelective(pmsProductSaleAttrValue);
            }
        }

        //添加图片列表
        List<PmsProductImage> spuImageList = pmsProductInfo.getSpuImageList();
        for (PmsProductImage pmsProductImage : spuImageList) {
            pmsProductImage.setProductId(spuId);
            pmsProductImageMapper.insertSelective(pmsProductImage);
        }

    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage pmsProductImage = new PmsProductImage();
        pmsProductImage.setProductId(spuId);
        List<PmsProductImage> pmsProductImageList = pmsProductImageMapper.select(pmsProductImage);
        return pmsProductImageList;
    }

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        String spuId = pmsSkuInfo.getSpuId();
        pmsSkuInfo.setProductId(spuId);
        //保存了pmsSkuInfo以后，会自动产生一个id，这个id就是skuId
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        //获取skuId，因为要存skuImage，和skuSaleAttr、skuSaleAttrValue
        String skuId = pmsSkuInfo.getId();

        //保存skuImageList
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            //根据skuId存skuImage
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }

        //保存skuAttrValue
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        //保存skuSaleAttrValueList
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
    }

    @Override
    public PmsSkuInfo getSkuInfo(String skuId) {
        /**
         * 查PmsSkuInfo这个bean，发现它里头有三个游离态属性，是需要根据spuId查其他表的
         * skuImageList  skuAttrValueList（中间表）  skuSaleAttrValueList
         * redis缓存版
         */
        PmsSkuInfo skuInfo = null;
        //先查缓存
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            //String ping = jedis.ping();
            //System.out.println(ping);
            //创建一个key object:skuId:field
            String skuKey = "sku:" + skuId + ":info";
            //根据key值去redis中取value
            String skuInfoJson = jedis.get(skuKey);
            //凡从数据库中取出来的  都要进行判断
            if (!StringUtils.isEmpty(skuInfoJson)) {
                //说明有这个skuId对应的skuInfo  将json串转换为skuInfo对象
                skuInfo = JSON.parseObject(skuInfoJson, PmsSkuInfo.class);
                /** 成功获得缓存数据 */
            } else {
                /** 说明要查的缓存数据不存在 */
                //先领号，领取分布式锁
                String lock = "sku:"+skuId+":lock";
                String uuid = UUID.randomUUID().toString();
                String OK = jedis.set(lock,uuid,"nx","px",10000);
                if ((!StringUtils.isEmpty(OK))&&OK.equals("OK")){
                    //加锁成功，调取数据库
                    skuInfo = getSkuInfoDB(skuId);
                    //存入缓存  先判断一下查询的是不是空
                    if (skuInfo != null){
                        /**
                         * 走到这里，说明没能在redis中查到缓存数据，但是领到了分布式锁
                         * 于是去调用了数据库，并将查出来的结果存入redis，并删除自己的锁
                         * */
                        jedis.set(skuKey,JSON.toJSONString(skuInfo));
                        //利用脚本语言（redis自己支持的）进行删除锁
                        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                        jedis.eval(script,Collections.singletonList(lock),Collections.singletonList(uuid));

                    } else {
                        /** 说明数据库中没有这个数据 */
                    }

                } else {
                    /** 走到这里说明没能在redis中查到缓存数据，也没能领取到分布式锁，开始自旋 */
                    return getSkuInfo(skuId);
                }
            }
        } catch (Exception e) {
            /** 通常调用一个打印日志的服务，将异常信息、报错时间、skuId打印 */
        } finally {
            //找到后关闭jedis
            jedis.close();
        }
        return skuInfo;
    }

    @Override
    public PmsSkuInfo getSkuInfoDB(String skuId) {
        /**
         * 查PmsSkuInfo这个bean，发现它里头有三个游离态属性，是需要根据spuId查其他表的
         * skuImageList  skuAttrValueList（中间表）  skuSaleAttrValueList
         */

        //根据skuId提取PmsSkuInfo
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(skuId);
        if (pmsSkuInfo==null){
            return null;
        }
        String spuId = pmsSkuInfo.getSpuId();

        //根据skuId提取pmsSkuImage
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImageList = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo.setSkuImageList(pmsSkuImageList);

        return pmsSkuInfo;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String skuId, String spuId) {
        List<PmsProductSaleAttr> pmsProductSaleAttrList = spuSaleAttrListCheckBySkuMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
        return pmsProductSaleAttrList;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheck(String skuId, String spuId) {
        //根据spuId查出spuSaleAttr
        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrList = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        for (PmsProductSaleAttr productSaleAttr : pmsProductSaleAttrList) {
            String saleAttrId = productSaleAttr.getSaleAttrId();
            //再根据spuSaleAttr里面的attr_id、product_id查出spuSaleAttrValue
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setSaleAttrId(saleAttrId);
            pmsProductSaleAttrValue.setProductId(spuId);
            List<PmsProductSaleAttrValue> productSaleAttrValueList = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);

            //遍历PmsProductSaleAttrValue，获取sale_attr_id、id
            for (PmsProductSaleAttrValue productSaleAttrValue : productSaleAttrValueList) {
                String id = productSaleAttrValue.getId();
                String saleAttrId1 = productSaleAttrValue.getSaleAttrId();
                //根据skuId查出遍历PmsSkuSaleAttrValue，遍历PmsSkuSaleAttrValue，并判断sale_attr_id、id
                PmsSkuSaleAttrValue pmsSkuSaleAttrValue = new PmsSkuSaleAttrValue();
                pmsSkuSaleAttrValue.setSkuId(skuId);
                List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValueList = pmsSkuSaleAttrValueMapper.select(pmsSkuSaleAttrValue);
                for (PmsSkuSaleAttrValue skuSaleAttrValue : pmsSkuSaleAttrValueList) {
                    if (skuSaleAttrValue.getSaleAttrValueId()==id && skuSaleAttrValue.getSaleAttrId()==saleAttrId1){
                        //如果相等，则给isCheck置为1
                        productSaleAttrValue.setIsChecked("1");
                    } else {
                        //如果不相等，则给isCheck置为0
                        productSaleAttrValue.setIsChecked("0");
                    }
                }
            }

            //这个时候，三表联查已经完成
            productSaleAttr.setSpuSaleAttrValueList(productSaleAttrValueList);

        }
        return pmsProductSaleAttrList;
    }

    @Override
    public List<PmsSkuInfo> getSkuInfoListBySpu(String spuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setProductId(spuId);
        List<PmsSkuInfo> skuInfoList = pmsSkuInfoMapper.select(pmsSkuInfo);
        for (PmsSkuInfo skuInfo : skuInfoList) {
            String skuId = skuInfo.getId();
            PmsSkuSaleAttrValue pmsSkuSaleAttrValue = new PmsSkuSaleAttrValue();
            pmsSkuSaleAttrValue.setSkuId(skuId);
            List<PmsSkuSaleAttrValue> saleAttrValueList = pmsSkuSaleAttrValueMapper.select(pmsSkuSaleAttrValue);
            skuInfo.setSkuSaleAttrValueList(saleAttrValueList);
        }
        return skuInfoList;
    }

    /**
     * getSkuForSearch()：查出所有的skuInfo信息
     * 目的是为了将数据库里面的skuInfo转换成searchSkuInfo
     * PUT到es中
     * */
    @Override
    public List<PmsSkuInfo> getSkuForSearch() {
        List<PmsSkuInfo> skuInfoList = pmsSkuInfoMapper.selectAll();
        //对比PmsSearchSkuInfo,有个skuAttrValue必须传
        for (PmsSkuInfo pmsSkuInfo : skuInfoList) {
            String skuInfoId = pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuInfoId);
            List<PmsSkuAttrValue> skuAttrValueList = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(skuAttrValueList);
        }
        return skuInfoList;
    }

    /**
     *
     * @param productSkuId
     * @return PmsSkuInfo
     * 本方法用于根据skuid查询PmsSkuInfo
     */
    @Override
    public PmsSkuInfo getProductBySkuId(String productSkuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        return skuInfo;
    }


}
