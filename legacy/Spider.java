import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xwhqs on 2016/3/15.
 */
public class Spider {
    static String SentGet(String url) {
        //定义一个字符串用来存储网页内容
        String result = "";

        //定义一个缓冲字符输入流
        BufferedReader in = null;

        try {
            //将string转换成url对象
            URL resultUrl = new URL(url);

            //初始化一个链接到那个url的连接
            URLConnection connection = resultUrl.openConnection();

            //开始实际的连接
            connection.connect();

            //初始化BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            //用来临时存储抓取到的每一行的数据
            String line;
            while ((line = in.readLine()) != null) {
                //遍历抓取到的每一行并将其存取到result里面
                result += line;
            }

        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }

        //使用finally来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        return result;
    }

    static ArrayList<ZhiHu> GetZhihu(String content) {

        //预定义一个ArrayList来存储结果
        ArrayList<ZhiHu> results = new ArrayList<>();

        //用来匹配url,即问题的链接
        Pattern pattern = Pattern.compile
                ("<h2>.+?question_link.+?href=\"" +
                        "(.+?)\".+?</h2>");
        Matcher matcher = pattern.matcher(content);

        //是否成功匹配成功的对象
        Boolean isFind = matcher.find();

        //如果找到了
        while (isFind) {
            //定义一个知乎对象来存储抓取的信息
            ZhiHu zhihuTemp = new ZhiHu(matcher.group(1));

            //添加成功匹配的成果
            results.add(zhihuTemp);

            //继续查找下一个对象
            isFind = matcher.find();
        }

        return results;
    }

}
