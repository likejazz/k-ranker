K-Ranker
========

K-Ranker is an algorithm that extracts keyword based on the order of priority.

**NOTICE: This document is written in Korean.**

K-Ranker는 우선순위에 따른 키워드 추출 알고리즘이다.

변곡점(inflection point)을 중심으로 수식1 / 수식2로 구분하여 두 개의 수식으로 관리하며 2차 방정식으로 포물선(parabolas: vertex form) 그래프를 구성한다. 각각의 수식은 y = a(x - m)^2 + n 을 기본으로 하며 inflection point의 위치에 따라 a 값을 유동적으로 조정해 포물선의 기울기에 변화를 준다.

- 블로그: http://dev.likejazz.com/post/94529154236/curve-fitting

초기에 구성했던 블로그 문서의 내용과는 다소 변경사항이 있으므로 보다 자세한 사항은 상세하게 주석을 기입한 코드를 참고하기 바란다 또는 아래와 같이 간단히 사용할 수 있다.

```java
    
    KRanker kranker = KRanker.getInstance();
    
    kranker.add("키워드 1", 1);
    kranker.add("키워드 2", 2);
    kranker.add("키워드 3", 2);
    kranker.add("키워드 4", 3);
    
    System.out.println(kranker.get());
```
