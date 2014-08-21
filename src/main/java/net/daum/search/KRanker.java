package net.daum.search;

import java.util.ArrayList;
import java.util.List;

public class KRanker {

    private static KRanker kranker = null;
    private int _DEFAULT_INFLECTION_POINT = 30; // 기본 변곡점(inflection point)은 30분

    List<Keyword> keywords = new ArrayList<Keyword>();

    protected KRanker() {}

    public static KRanker getInstance() {
        if (kranker == null) {
            kranker = new KRanker();
        }
        return kranker;
    }

    /**
     * 현재시각의 epoch time을 분 단위로 구한다.
     * @return  현재시각의 epoch time 분 단위
     */
    private long getCurrentEpochMins() {
        return System.currentTimeMillis() / 1000 / 60;
    }

    /**
     * 현재시각을 유입시점으로 키워드를 입력한다.
     * @param keyword   키워드
     * @param rank      등 수
     */
    public void add(String keyword, int rank) {
        this.add(keyword, rank, this.getCurrentEpochMins(), false);
    }

    /**
     * 특정시각을 유입시점으로 키워드를 입력한다.
     * @param keyword   키워드
     * @param rank      등 수
     * @param epoch_min 특정시각의 분 단위 epoch time
     */
    public void add(String keyword, int rank, long epoch_min) {
        this.add(keyword, rank, epoch_min, false);
    }

    /**
     * 특정시각, 덮어쓰기 유무를 입력받아 키워드를 입력한다.
     * @param keyword   키워드
     * @param rank      등 수
     * @param epoch_min 특정시각의 분 단위 epoch time
     * @param override  덮어쓰기 유무, true 인 경우 동일한 키워드가 있으면 덮어쓴다.
     */
    public void add(String keyword, int rank, long epoch_min, boolean override) {
        for (Keyword k : keywords) {
            if (keyword.equals(k.getKeyword())) {
                if (override) {
                    keywords.remove(k);
                    keywords.add(new Keyword(keyword, rank, epoch_min));
                }

                return;
            }
        }

        keywords.add(new Keyword(keyword, rank, epoch_min));
    }

    /**
     * 키워드를 추출한다.
     * @return              가장 점수가 높은 키워드
     * @throws Exception    점수 계산시 잘못된 inflection point가 유입되면 오류 발생
     */
    public String get() throws Exception {
        return this.get(_DEFAULT_INFLECTION_POINT);
    }

    /**
     * inflection point를 지정하여 키워드를 추출한다.
     * @param inflection_point  변곡점(inflection point)을 유동적으로 입력 받는다. 시간대별 상이하게 받을 수 있다. e.g. 낮에는 30, 밤에는 60 등등.
     * @return                  가장 점수가 높은 키워드
     * @throws Exception        점수 계산시 잘못된 inflection point가 유입되면 오류 발생
     */
    public String get(int inflection_point) throws Exception {
        float score;
        float maxScore = 0;
        Keyword maxKeyword = null;

        for (Keyword k: keywords) {
            // 종합 점수 계산
            score = calc(k.getRank(), k.getEpochMin(), inflection_point);

            if (score >= maxScore) {
                maxScore = score;
                maxKeyword = k;
            }
        }

        // 선정된 키워드가 없거나 키워드 리스트가 비어있을 경우
        if (maxKeyword == null) {
            return null;
        } else {
            // 선정된 키워드는 리스트에서 제외
            keywords.remove(maxKeyword);

            return maxKeyword.getKeyword();
        }
    }

    // 키워드 리스트를 모두 삭제한다.
    public void removeAll() {
        keywords.removeAll(keywords);
    }

    /**
     * 종합 점수를 계산한다.
     * @param rank              등 수
     * @param epoch_min         키워드 유입시점의 epoch time 분 단위
     * @param inflection_point  최고 점수를 받는 inflection point
     * @return                  종합 점수
     * @throws Exception        점수 계산시 잘못된 inflection point가 유입되면 오류 발생
     */
    public float calc(int rank, long epoch_min, int inflection_point) throws Exception {
        float score1    = 0;
        float score2    = 0;

        double a1;
        double a2;

        long diff = this.getCurrentEpochMins() - epoch_min;

        /**
         * 변곡점(inflection point)은 0분 ~ 120분까지 10분 단위로 지정할 수 있다.
         * 각 단위마다 미리 계산한 a1, a2 값을 할당받아 포물선(parabolas: vertex form) 그래프에 따라
         * 점수를 부여 받으며 변곡점을 잘못 지정한 경우 Exception을 발생하여 실행되지 않도록 했다.
         */
        switch(inflection_point) {
            case 0:     a1 = 1;         a2 = 0.0031;    break;
            case 10:    a1 = -0.8;      a2 = 0.0034;    break;
            case 20:    a1 = -0.2;      a2 = 0.0039;    break;
            case 30:    a1 = -0.089;    a2 = 0.00445;   break;
            case 40:    a1 = -0.05;     a2 = 0.0051;    break;
            case 50:    a1 = -0.032;    a2 = 0.0059;    break;
            case 60:    a1 = -0.0222;   a2 = 0.007;     break;
            case 70:    a1 = -0.0163;   a2 = 0.0083;    break;
            case 80:    a1 = -0.0125;   a2 = 0.01;      break;
            case 90:    a1 = -0.0098;   a2 = 0.0124;    break;
            case 100:   a1 = -0.008;    a2 = 0.0156;    break;
            case 110:   a1 = -0.0066;   a2 = 0.0204;    break;
            case 120:   a1 = -0.0055;   a2 = 0.0278;    break;

            default:    throw new Exception("An inflection point is invalid.");
        }

        score1 = calc1(rank, inflection_point, diff, a1, a2);   // 시간 점수
        score2 = calc2(rank);                                   // 랭킹 점수

        // 시간 점수 97% + 랭킹 점수 3%
        // 총점은 계산하기 쉽게 100점으로 정한다.
        return (float) (score1 * 0.97 + score2 * 0.03);
    }

    // 시간 점수
    private float calc1(int rank, int inflection_point, long diff_min, double a1, double a2) {
        float score1;

        /**
         * diff_min 이 0 ~ inflection_point 구간이면 수식1,
         * 180 이하이면 수식2,
         * 그외는 모두 0점 처리한다.
         * 수식2에서는 180(3시간)분에 0점으로 수렴한다. 따라서 3시간 이후는 모두 0점이다.
         */
        if (diff_min >= 0 &&
                diff_min <= inflection_point) {
            score1 = (float) (a1 * Math.pow(diff_min - inflection_point, 2) + 100);
        } else if (diff_min <= 180) {
            score1 = (float) (a2 * Math.pow(diff_min - 180, 2));
        } else {
            score1 = 0;
        }

        // normalize
        score1 = (score1 > 100) ? 100 : score1;
        score1 = (score1 < 0) ? 0 : score1;

        return score1;
    }

    // 랭킹 점수
    private float calc2(float rank) {
        float score2;

        /**
         * 랭킹이 1위일때 100점,
         * 이후 등수에 따라 10%씩 감산한다.
         * 10위를 벗어나면 0점 처리한다.
         */
        if (rank >= 1 &&
                rank <= 10) {
            score2 = (11 - rank) / 10 * 100;
        } else {
            score2 = 0;
        }

        return score2;
    }

    @Override
    public String toString() {
        String out = ""; // StringUtils.EMPTY

        for (Keyword k : keywords) {
            float score = 0;
            try {
                score = calc(k.getRank(), k.getEpochMin(), _DEFAULT_INFLECTION_POINT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            out += "Keyword [" +
                        "keyword=" + k.getKeyword() + "," +
                        "epoch_min=" + k.getEpochMin() + "," +
                        "rank=" + k.getRank() + "," +
                        "diff=" + (this.getCurrentEpochMins() - k.getEpochMin()) + "," +
                        "score=" + score +
                    "]\n";
        }
        return out;
    }

    // 키워드 정보를 보관하는 `Keyword` InnerClass
    private class Keyword {

        private String keyword;
        private long epoch_min;
        private int rank;

        public Keyword(String keyword, int rank, long epoch_time) {
            this.setKeyword(keyword);
            this.setRank(rank);
            this.setEpochMin(epoch_time);
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public long getEpochMin() {
            return epoch_min;
        }

        public void setEpochMin(long epoch_min) {
            this.epoch_min = epoch_min;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }
}