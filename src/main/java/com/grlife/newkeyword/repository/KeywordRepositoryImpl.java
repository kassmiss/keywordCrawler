package com.grlife.newkeyword.repository;

import com.grlife.newkeyword.util.PropertiesLoader;
import com.grlife.newkeyword.util.RestClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.*;

public class KeywordRepositoryImpl implements KeywordRepository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KeywordRepositoryImpl(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    private String API_FLAG = "R"; // API enabled
    private int connNo = 1;

    @Override
    public void startKeyword() {

        String keyword = "";

        if("".equals(keyword)) {

            keyword = getKeyword();

            if("".equals(keyword)) {
                keyword = "효능";
            }

        }

        Properties properties = null;

        try {

            properties = PropertiesLoader.fromResource("api.properties");

            String clientId1 = properties.getProperty("clientId1");
            String clientSecret1 = properties.getProperty("clientSecret1");
            String clientId2 = properties.getProperty("clientId2");
            String clientSecret2 = properties.getProperty("clientSecret2");
            String clientId3 = properties.getProperty("clientId3");
            String clientSecret3 = properties.getProperty("clientSecret3");
            String clientId4 = properties.getProperty("clientId4");
            String clientSecret4 = properties.getProperty("clientSecret4");
            String clientId5 = properties.getProperty("clientId5");
            String clientSecret5 = properties.getProperty("clientSecret5");
            String clientId6 = properties.getProperty("clientId6");
            String clientSecret6 = properties.getProperty("clientSecret6");
            String clientId7 = properties.getProperty("clientId7");
            String clientSecret7 = properties.getProperty("clientSecret7");

            String baseUrl = properties.getProperty("BASE_URL");
            String apiKey = properties.getProperty("API_KEY");
            String secretKey = properties.getProperty("SECRET_KEY");
            long customerId = Long.parseLong(properties.getProperty("CUSTOMER_ID"));
            RestClient rest = RestClient.of(baseUrl, apiKey, secretKey);
            String keywordsPath = "/keywordstool";

            // API 호출
            HttpResponse<?> response = rest.get(keywordsPath, customerId).queryString("hintKeywords", keyword)
                    .queryString("includeHintKeywords", Integer.valueOf(1))
                    .queryString("showDetail", Integer.valueOf(1)).asString();

            String keyBody = (String) response.getBody();
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(keyBody);
            JSONArray keywordListJson = (JSONArray) jsonObject.get("keywordList");

            if(keywordListJson != null) {

                List<String> keywordList = new ArrayList<String>();
                List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();

                int insertCnt = 0;

                for (int i = 0; i < keywordListJson.size(); i++) {
                    JSONObject data = (JSONObject) keywordListJson.get(i);

                    keywordList.add((String) data.get("relKeyword"));
                }

                List<Map<String, Object>> selectList = selectKeywordExistList(keywordList);

                /* 중복 대상 키워드 추출 및 제외 대상으로 등록 */
                Map<String, Object> resultMap = new HashMap<String, Object>();
                for(int j = 0; j<selectList.size(); j++) {
                    resultMap.put(selectList.get(j).get("KEYWORD").toString(), "KEYWORD");
                }

                for(int i=0; i<keywordListJson.size(); i++) {
                    JSONObject data = (JSONObject) keywordListJson.get(i);
                    String relKeyword = data.get("relKeyword").toString();

                    if(resultMap.get(relKeyword) == null) {
                        try {
                            // Keyword System Response

                            String text = URLEncoder.encode(relKeyword, "UTF-8");
                            String apiURL = "https://openapi.naver.com/v1/search/blog?query=" + text; // json 결과
                            URL url = new URL(apiURL);
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            con.setRequestMethod("GET");

                            if (connNo == 1) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId1);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret1);
                            } else if (connNo == 2) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId2);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret2);
                            } else if (connNo == 3) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId3);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret3);
                            } else if (connNo == 4) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId4);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret4);
                            } else if (connNo == 5) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId5);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret5);
                            } else if (connNo == 6) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId6);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret6);
                            } else if (connNo == 7) {
                                con.setRequestProperty("X-Naver-Client-Id", clientId7);
                                con.setRequestProperty("X-Naver-Client-Secret", clientSecret7);
                            }

                            int responseCode = con.getResponseCode();

                            if(responseCode == 200) {

                                Double pcVal = 0.0;
                                Double mbVal = 0.0;
                                int blogLocVal = 0;


                                insertCnt++;
                                logger.debug("DEBUG :: [" + connNo + "] :: " + relKeyword);

                                Map<String, Object> map = new HashMap<String, Object>();

                                map.put("keyword", relKeyword);
                                map.put("pcCnt", data.get("monthlyPcQcCnt"));
                                map.put("mbCnt", data.get("monthlyMobileQcCnt"));
                                map.put("pcClkAvgCnt", data.get("monthlyAvePcClkCnt"));
                                map.put("mbClkAvgCnt", data.get("monthlyAveMobileClkCnt"));
                                map.put("pcClkPer", data.get("monthlyAvePcCtr"));
                                map.put("mbClkPer", data.get("monthlyAveMobileCtr"));
                                map.put("avgDepth", data.get("plAvgDepth"));
                                map.put("compIdx", data.get("compIdx"));

                                //if(setTimeout(70)) {

                                try
                                {

                                    BufferedReader br;

                                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));

                                    StringBuffer responseData = new StringBuffer();
                                    String inputLine;
                                    while((inputLine = br.readLine()) != null) {
                                        responseData.append(inputLine);
                                    }

                                    br.close();
                                    JSONParser jsonParserData = new JSONParser();
                                    JSONObject jsonObjectData = new JSONObject();
                                    jsonObjectData = (JSONObject)jsonParserData.parse(responseData.toString());

                                    String docCnt = "";
                                    try {
                                        docCnt = jsonObjectData.get("total").toString();
                                    } catch(Exception e) {
                                        docCnt = "0";
                                    }

                                    // PC Value
                                    if(!"0".equals(docCnt) && !"< 10".equals(map.get("pcCnt").toString())) {
                                        Double pcCnt = Double.parseDouble(map.get("pcCnt").toString());
                                        map.put("pcVal", Math.floor((calc(Double.parseDouble(docCnt)) - calc(pcCnt)) * 10) / 10.0);
                                        //pcVal = Math.round((calc(Double.parseDouble(docCnt)) - calc(pcCnt)) * 10) / 10.0;

                                    } else {
                                        map.put("pcVal", "0.0");
                                    }

                                    // Mobile Value
                                    if(!"0".equals(docCnt) && !"< 10".equals(map.get("mbCnt").toString())) {
                                        Double mbCnt = Double.parseDouble(map.get("mbCnt").toString());
                                        //map.put("mbVal", Math.round((calc(Double.parseDouble(docCnt)) - calc(mbCnt)) * 10) / 10.0);
                                        mbVal = Math.floor((calc(Double.parseDouble(docCnt)) - calc(mbCnt)) * 10) / 10.0;
                                    } else {
                                        //map.put("mbVal", "0.0");
                                    }


                                    map.put("docCnt", docCnt);


                                    // Mobile Location
                                    Document doc = Jsoup.connect("https://m.search.naver.com/search.naver?sm=top_hty&fbm=1&ie=utf8&query=" + URLEncoder.encode(relKeyword, "UTF-8") ).get();

                                    Elements els_tmp = doc.select("#place-app-root");

                                    map.put("blogLoc", "0");
                                    map.put("blogLocTmp", "");

                                    String blogType = "";

                                    if(els_tmp.size() > 0) {

                                        els_tmp = doc.select("section.sc, #place-app-root");

                                        for(int j=0; j<els_tmp.size(); j++) {

                                            if(els_tmp.get(j).toString().indexOf("sp_nreview ") > 0){
                                                map.put("blogLoc", j+1);
                                                blogLocVal = j+1;
                                            }

                                        }

                                        map = getBlogType(els_tmp.get(0).toString(), map);

                                    } else {

                                        List<String> els = doc.select("section.sc").eachAttr("class");

                                        for(int j=0; j<els.size(); j++) {

                                            if(els.get(j).indexOf("sp_nreview ") > 0){
                                                map.put("blogLoc", j+1);
                                                blogLocVal = j+1;
                                            }

                                        }

                                        map = getBlogType(els.get(0), map);

                                    }

                                    mbVal =  Math.floor(mbVal * 10) / 10.0;

                                    map.put("mbVal", mbVal);


                                }
                                catch(Exception e) {
                                    e.printStackTrace();
                                }


                                if(map != null && "R".equals(API_FLAG)) {
                                    returnList.add(map);
                                }

                                //}

                            } else if(responseCode == 429){

                                if(connNo < 7) {
                                    if(!Thread.interrupted()) {
                                        connNo++;
                                    }
                                } else {
                                    if(!Thread.interrupted()) {
                                        API_FLAG = "F";
                                    }
                                }

                                break;

                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                logger.debug("DEBUG :: INSERT :: " + insertCnt);

                // DB Insert & Update
                if(returnList != null && "R".equals(API_FLAG)) {

                    String sql = "INSERT INTO KEYWORD(KEYWORD, BLOG_CATE, BLOG_CATE_NO, BLOG_CATE_TMP, BLOG_LOC, DOC_CNT, MB_VAL, PC_VAL, MB_CNT, PC_CNT) VALUES(:KEYWORD, :BLOG_CATE, :BLOG_CATE_NO, :BLOG_CATE_TMP, :BLOG_LOC, :DOC_CNT, :MB_VAL, :PC_VAL, :MB_CNT, :PC_CNT)";
                    MapSqlParameterSource params = null;

                    if(returnList.size() != 0) { // 결과 목록 저장

                        for(Map<String, Object> map : returnList) {
                            try {
                                params = new MapSqlParameterSource();
                                params.addValue("KEYWORD", map.get("keyword"));

                                params.addValue("BLOG_CATE", map.get("blogCate"));
                                params.addValue("BLOG_CATE_NO", map.get("blogCateNo"));
                                params.addValue("BLOG_CATE_TMP", map.get("blogCateTmp"));
                                params.addValue("BLOG_LOC", map.get("blogLoc"));
                                params.addValue("DOC_CNT", map.get("docCnt"));
                                params.addValue("MB_VAL", map.get("mbVal"));
                                params.addValue("PC_VAL", map.get("pcVal"));
                                params.addValue("MB_CNT", map.get("mbCnt"));
                                params.addValue("PC_CNT", map.get("pcCnt"));

                                jdbcTemplate.update(sql, params);
                            } catch (DuplicateKeyException e) {
                                // 중복 패스!
                            }
                        }

                    }

                    String updateSql = "UPDATE KEYWORD SET USED = 1 WHERE KEYWORD = :KEYWORD";
                    MapSqlParameterSource updateParams = new MapSqlParameterSource();
                    updateParams.addValue("KEYWORD", keyword);
                    jdbcTemplate.update(updateSql, updateParams);

                } else {

                    logger.debug("DEBUG :: API 사용 종료");

                }

            }

        }catch (UnirestException e) {

        }catch (ParseException e) {

        }catch (GeneralSecurityException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void reset() {
        API_FLAG = "R";
        connNo = 1;
    }

    private String getKeyword() {

        String sql = "SELECT KEYWORD FROM KEYWORD WHERE USED = 0 LIMIT 1";
        String tmp = "";
        try {
            tmp = jdbcTemplate.queryForObject(sql, new HashMap<String, Object>(), String.class);
        } catch(EmptyResultDataAccessException e) {}

        return tmp;


    }

    private List<Map<String, Object>> selectKeywordExistList(List<String> list) {
        
        String sql = "SELECT * FROM KEYWORD WHERE KEYWORD IN (:lastnamevalues)";

        Map namedParameters = Collections.singletonMap("lastnamevalues", list);
        StringBuffer recordQueryString = new StringBuffer();

        List<Map<String, Object>> rsList = jdbcTemplate.query(sql, namedParameters, rowMapper());

        return rsList;

    }

    private RowMapper<Map<String, Object>> rowMapper() {
        return (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("KEYWORD", rs.getString("KEYWORD"));
            return map;
        };
    }
    
    private Map<String, Object> getBlogType(String text, Map<String, Object> kMap) {

        /* 블로그 */
        if(text.indexOf("sp_nreview") > 0) {
            kMap.put("blogCate", "블로그(VIEW)");
            kMap.put("blogCateNo", "0000");

        } else if(text.indexOf("sp_influencer") > 0) {
            kMap.put("blogCate", "인플루언서");
            kMap.put("blogCateNo", "0001");
        } else if(text.indexOf("sp_ncafe_used") > 0) {
            kMap.put("blogCate", "카페글");
            kMap.put("blogCateNo", "0002");

            /* 파워링크 */
        } else if(text.indexOf("sp_power") > 0) {
            kMap.put("blogCate", "파워링크");
            kMap.put("blogCateNo", "0100");
        } else if(text.indexOf("nx_fashion") > 0) {
            kMap.put("blogCate", "파워링크 상품");
            kMap.put("blogCateNo", "0101");
        } else if(text.indexOf("nx_pla") > 0) {
            kMap.put("blogCate", "파워상품");
            kMap.put("blogCateNo", "0102");

            /* 사전 */
        } else if(text.indexOf("sp_nkindic") > 0) {
            kMap.put("blogCate", "지식백과");
            kMap.put("blogCateNo", "0200");
        } else if(text.indexOf("sp_ndic") > 0) {
            kMap.put("blogCate", "사전");
            kMap.put("blogCateNo", "0201");

            /* 동영상 */
        } else if(text.indexOf("sp_nvideo") > 0) {
            kMap.put("blogCate", "동영상");
            kMap.put("blogCateNo", "0300");

            /* 네이버 */
        } else if(text.indexOf("sp_nshop") > 0) {
            kMap.put("blogCate", "네이버 쇼핑");
            kMap.put("blogCateNo", "0400");
        } else if(text.indexOf("nx_hotel") > 0) {
            kMap.put("blogCate", "네이버 호텔예약");
            kMap.put("blogCateNo", "0401");
        } else if(text.indexOf("sp_nland") > 0) {
            kMap.put("blogCate", "네이버 부동산");
            kMap.put("blogCateNo", "0402");
        } else if(text.indexOf("sp_nbook") > 0) {
            kMap.put("blogCate", "네이버책");
            kMap.put("blogCateNo", "0403");
        } else if(text.indexOf("cs_nbook") > 0) {
            kMap.put("blogCate", "네이버책");
            kMap.put("blogCateNo", "0404");
        } else if(text.indexOf("sp_nweb") > 0) {
            kMap.put("blogCate", "사이트");
            kMap.put("blogCateNo", "0405");
        } else if(text.indexOf("sp_ntotal") > 0) { // 지식인
            kMap.put("blogCate", "지식iN");
            kMap.put("blogCateNo", "0406");
        } else if(text.indexOf("cs_jrnaver") > 0) {
            kMap.put("blogCate", "주니어네이버");
            kMap.put("blogCateNo", "0407");
        } else if(text.indexOf("sp_shop_gift") > 0) {
            kMap.put("blogCate", "네이버선물");
            kMap.put("blogCateNo", "0408");

            /* 부동산 */
        } else if(text.indexOf("cs_land_calculator") > 0) {
            kMap.put("blogCate", "부동산 중개보수 계산기");
            kMap.put("blogCateNo", "0500");

            /* 앱 */
        } else if(text.indexOf("cs_app_info") > 0) {
            kMap.put("blogCate", "앱정보");
            kMap.put("blogCateNo", "0600");

            /* 자동차 */
        } else if(text.indexOf("cs_manufacturer") > 0) {
            kMap.put("blogCate", "자동차 정보");
            kMap.put("blogCateNo", "0700");
        } else if(text.indexOf("cs_car_model_template") > 0) {
            kMap.put("blogCate", "자동차모델");
            kMap.put("blogCateNo", "0701");
        } else if(text.indexOf("cs_motorshow") > 0) {
            kMap.put("blogCate", "모터쇼");
            kMap.put("blogCateNo", "0702");
        } else if(text.indexOf("cs_motorcycle") > 0) {
            kMap.put("blogCate", "모터사이클 정보");
            kMap.put("blogCateNo", "0703");
        } else if(text.indexOf("cs_sale_condition") > 0) {
            kMap.put("blogCate", "자동차 판매조건");
            kMap.put("blogCateNo", "0704");

            /* 육아 */
        } else if(text.indexOf("mcs_infant_care") > 0) {
            kMap.put("blogCate", "아기정보");
            kMap.put("blogCateNo", "0800");

            /* 교육 */
        } else if(text.indexOf("cs_examgrade") > 0) {
            kMap.put("blogCate", "전국연합학력평가");
            kMap.put("blogCateNo", "0900");
        } else if(text.indexOf("cs_textbook_music") > 0) {
            kMap.put("blogCate", "교재 음악");
            kMap.put("blogCateNo", "0901");

            /* 지도 */
        } else if(text.indexOf("cs_fastway") > 0) {
            kMap.put("blogCate", "빠른길찾기");
            kMap.put("blogCateNo", "1000");
        } else if(text.indexOf("cs_bus_info") > 0) {
            kMap.put("blogCate", "버스정보");
            kMap.put("blogCateNo", "1001");
        } else if(text.indexOf("cs_train_schd") > 0) {
            kMap.put("blogCate", "역정보");
            kMap.put("blogCateNo", "1002");

            /* 기본 */
        } else if(text.indexOf("cs_samecontent") > 0) {
            kMap.put("blogCate", "동일검색어");
            kMap.put("blogCateNo", "1100");
        } else if(text.indexOf("sp_nsite") > 0) {
            kMap.put("blogCate", "사이트");
            kMap.put("blogCateNo", "1101");
        } else if(text.indexOf("sp_nnews") > 0) {
            kMap.put("blogCate", "뉴스");
            kMap.put("blogCateNo", "1102");
        } else if(text.indexOf("sp_nimage") > 0) {
            kMap.put("blogCate", "이미지");
            kMap.put("blogCateNo", "1103");
        } else if(text.indexOf("sp_nrealtime") > 0) {
            kMap.put("blogCate", "실시간 검색");
            kMap.put("blogCateNo", "1104");
        } else if(text.indexOf("place-app-root") > 0) {
            kMap.put("blogCate", "장소");
            kMap.put("blogCateNo", "1105");

            /* 공공 */
        } else if(text.indexOf("cs_civilcomplaint") > 0) {
            kMap.put("blogCate", "민원정보");
            kMap.put("blogCateNo", "1200");

            /* 인물, 연예인 */
        } else if(text.indexOf("cs_npeople_profile") > 0) {
            kMap.put("blogCate", "인물정보");
            kMap.put("blogCateNo", "1300");
        } else if(text.indexOf("cs_npeople_piece") > 0) {
            kMap.put("blogCate", "인물이력");
            kMap.put("blogCateNo", "1301");
        } else if(text.indexOf("cs_npeople_same") > 0) {
            kMap.put("blogCate", "동명이인");
            kMap.put("blogCateNo", "1302");
        } else if(text.indexOf("cs_star_profile") > 0) {
            kMap.put("blogCate", "연예인 프로필");
            kMap.put("blogCateNo", "1303");

            /* 여행, 공연, 활동, 축제 */
        } else if(text.indexOf("cs_activity_info") > 0) {
            kMap.put("blogCate", "청소년활동정보");
            kMap.put("blogCateNo", "1400");
        } else if(text.indexOf("cs_festival") > 0) {
            kMap.put("blogCate", "축제정보");
            kMap.put("blogCateNo", "1401");
        } else if(text.indexOf("cs_performance_list") > 0) {
            kMap.put("blogCate", "공연정보");
            kMap.put("blogCateNo", "1402");
        } else if(text.indexOf("cs_movie") > 0) {
            kMap.put("blogCate", "영화정보");
            kMap.put("blogCateNo", "1403");
        } else if(text.indexOf("cs_nart") > 0) {
            kMap.put("blogCate", "미술/전시정보");
            kMap.put("blogCateNo", "1404");
        } else if(text.indexOf("sp_map_top") > 0) {
            kMap.put("blogCate", "여행지추천");
            kMap.put("blogCateNo", "1405");
        } else if(text.indexOf("cs_attraction") > 0) {
            kMap.put("blogCate", "가볼만한곳");
            kMap.put("blogCateNo", "1406");
        } else if(text.indexOf("cs_recommend_course") > 0) {
            kMap.put("blogCate", "추천코스");
            kMap.put("blogCateNo", "1407");
        } else if(text.indexOf("cs_flight_info") > 0) {
            kMap.put("blogCate", "비행소요시간");
            kMap.put("blogCateNo", "1408");
        } else if(text.indexOf("flight_box") > 0) {
            kMap.put("blogCate", "항공권");
            kMap.put("blogCateNo", "1409");
        } else if(text.indexOf("sc cs") > 0) {
            kMap.put("blogCate", "정보알림");
            kMap.put("blogCateNo", "1410");

            /* 강좌 */
        } else if(text.indexOf("cs_listui") > 0) {
            kMap.put("blogCate", "온라인 공개 강좌");
            kMap.put("blogCateNo", "1500");

            /* 투자 */
        } else if(text.indexOf("cs_economy_chart") > 0) {
            kMap.put("blogCate", "투자정보");
            kMap.put("blogCateNo", "1600");
        } else if(text.indexOf("sp_company") > 0) {
            kMap.put("blogCate", "회사링크");
            kMap.put("blogCateNo", "1601");

            /* 기타 */
        } else if(text.indexOf("sp_nresult") > 0) {
            kMap.put("blogCate", "결과 더보기");
            kMap.put("blogCateNo", "1700");
        } else if(text.indexOf("cs_overseas_trip") > 0) {
            kMap.put("blogCate", "추가정보");
            kMap.put("blogCateNo", "1701");
        } else if(text.indexOf("cs_n_company_newest") > 0) {
            kMap.put("blogCate", "사이트 최신정보");
            kMap.put("blogCateNo", "1702");
        } else if(text.indexOf("cs_unitchg_new") > 0) {
            kMap.put("blogCate", "단위변환");
            kMap.put("blogCateNo", "1703");
        } else if(text.indexOf("sp_brand") > 0) {
            kMap.put("blogCate", "브랜드 광고");
            kMap.put("blogCateNo", "1704");
        } else if(text.indexOf("cs_howto") > 0) {
            kMap.put("blogCate", "하는법"); // 아이메이크업 하는 법
            kMap.put("blogCateNo", "1705");
        } else if(text.indexOf("cs_answering_engine") > 0) {
            kMap.put("blogCate", "구조물 측정");
            kMap.put("blogCateNo", "1706");
        } else if(text.indexOf("cs_cacode_kr") > 0) {
            kMap.put("blogCate", "지역번호[국내]");
            kMap.put("blogCateNo", "1707");
        } else if(text.indexOf("\"sp cs\"") > 0) {
            kMap.put("blogCate", "기타 분류"); // 계산기, 탄생석 (구분 좀더 세분화 필요)
            kMap.put("blogCateNo", "1708");
        } else if(text.indexOf("cs_calendar") > 0) {
            kMap.put("blogCate", "달력");
            kMap.put("blogCateNo", "1709");
        } else if(text.indexOf("cs_nlucky") > 0) {
            kMap.put("blogCate", "운세");
            kMap.put("blogCateNo", "1710");
        } else if(text.indexOf("cs_weather") > 0) {
            kMap.put("blogCate", "날씨");
            kMap.put("blogCateNo", "1711");
        } else if(text.indexOf("cs_season_food") > 0) {
            kMap.put("blogCate", "제철음식");
            kMap.put("blogCateNo", "1712");
        } else if(text.indexOf("cs_webtoon") > 0) {
            kMap.put("blogCate", "웹툰");
            kMap.put("blogCateNo", "1713");
        } else if(text.indexOf("sp_card") > 0) {
            kMap.put("blogCate", "카드");
            kMap.put("blogCateNo", "1714");
        } else if(text.indexOf("mcs_featured_snippet") > 0) {
            kMap.put("blogCate", "스니팻");
            kMap.put("blogCateNo", "1715");

        } else {
            if(text.length() > 100) {
                text = text.substring(0, 100);

            }

            kMap.put("blogCateTmp", text);
        }

        return kMap;

    }

    // Calculation Value
    private Double calc(Double cnt) {

        switch((int) (cnt/100)) {
            case 0 :
                return 5.0;
            case 1 :
                return 4.9;
            case 2 :
                return 4.8;
            case 3 :
                return 4.7;
            case 4 :
                return 4.6;
            case 5 :
                return 4.5;
            case 6 :
                return 4.4;
            case 7 :
                return 4.3;
            case 8 :
                return 4.2;
            case 9 :
                return 4.1;
            case 10 :
                return 4.0;
            case 11 :
                return 3.9;
            case 12 :
                return 3.8;
            case 13 :
                return 3.7;
            case 14 :
                return 3.6;
            case 15 :
                return 3.5;
            case 16 :
                return 3.4;
            case 17 :
                return 3.3;
            case 18 :
                return 3.2;
            case 19 :
                return 3.1;
            case 20 :
                return 3.0;
            case 21 :
                return 2.9;
            case 22 :
                return 2.8;
            case 23 :
                return 2.7;
            case 24 :
                return 2.6;
            case 25 :
                return 2.5;
            case 26 :
                return 2.4;
            case 27 :
                return 2.3;
            case 28 :
                return 2.2;
            case 29 :
                return 2.1;
            case 30 :
                return 2.0;
            case 31 :
                return 1.9;
            case 32 :
                return 1.8;
            case 33 :
                return 1.7;
            case 34 :
                return 1.6;
            case 35 :
                return 1.5;
            case 36 :
                return 1.4;
            case 37 :
                return 1.3;
            case 38 :
                return 1.2;
            case 39 :
                return 1.1;
            case 40 :
                return 1.0;
            case 41 :
                return 0.9;
            case 42 :
                return 0.8;
            case 43 :
                return 0.7;
            case 44 :
                return 0.6;
            case 45 :
                return 0.5;
            case 46 :
                return 0.4;
            case 47 :
                return 0.3;
            case 48 :
                return 0.2;
            case 49 :
                return 0.1;
            case 50 :
                return 0.0;
            default :
                return 0.0;


        }

    }
}
