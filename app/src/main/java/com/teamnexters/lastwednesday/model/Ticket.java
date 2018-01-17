package com.teamnexters.lastwednesday.model;

import java.util.Date;

import lombok.Data;
import lombok.NonNull;

/**
 * Created by JY on 2018-01-12.
 */

/**
 * 티켓 정보 담을 클래스
 */
@Data(staticConstructor = "of")
public class Ticket {

    @NonNull private String title;
    @NonNull private double price;
    @NonNull private Date showTime;

}
