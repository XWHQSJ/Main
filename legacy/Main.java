import java.util.ArrayList;

/**
 * Created by xwhqs on 2016/3/14.
 */
public class Main {

    public static void main(String[] args) {
        //定义即将访问的链接
        String url = "http://www.zhihu.com/explore/recommendations";

        //访问链接并获取页面内容
        String content = Spider.SentGet(url);

        //获取该页面的所有知乎对象
        ArrayList<ZhiHu> myZhihu = Spider.GetZhihu(content);

        //写入本地
        for (ZhiHu zhiHu : myZhihu) {
            FileReaderWriter.writeIntoFile(zhiHu.writeString(), "D:/知乎-编辑推荐.txt", true);

        }
    }
}



