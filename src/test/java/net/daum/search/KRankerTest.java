package net.daum.search;

import org.junit.Before;
import org.junit.Test;

public class KRankerTest {

    private KRanker kranker = null;

    @Before
    public void setUp() {
        // 키워드를 지속적으로 유지해야 하므로 singleton 처리
        kranker = KRanker.getInstance();
    }

    private long diff(int minutes) {
        return System.currentTimeMillis() / 1000 / 60 - minutes;
    }

    @Test
    public void test1() {
        // 키워드 추가
        kranker.add("키워드", 1);
        // 5분전 시점으로 키워드 추가
        kranker.add("5분전 입력으로 가정한 키워드", 1, diff(5));
        // 8위 키워드
        kranker.add("8위", 8);
        // 4위 키워드
        kranker.add("4위", 4);

        System.out.println(kranker.toString());
    }
}