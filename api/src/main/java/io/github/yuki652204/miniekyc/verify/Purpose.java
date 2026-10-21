package io.github.yuki652204.miniekyc.verify;

/**
 * 確認の目的。目的ごとに「返してよい情報」が違う（最小開示）。
 * 業種によって求められる確認の強さが違う、という話のミニチュア版。
 */
public enum Purpose {
    /** 酒類販売など：20歳以上かどうかだけ返す（生年月日は返さない） */
    AGE_CHECK,
    /** 地域アプリなど：都道府県だけ返す */
    PREFECTURE_CHECK
}
