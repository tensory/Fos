package com.erogear.android.fos;

import com.erogear.android.bluetooth.video.Frame;

/**
 * Ripped off from com.erogear.android.bluetooth.comm.MultiheadLayoutView
 * because it's really useful, and unfortunately private to that layout.
 * 
 * @author ari
 */
public class NumberFrame extends Frame {
    String[] nums = new String[] {
            "010"+
            "101"+
            "101"+
            "101"+
            "010",

            "010"+
            "110"+
            "010"+
            "010"+
            "010",

            "111"+
            "001"+
            "111"+
            "100"+
            "111",

            "111"+
            "001"+
            "111"+
            "001"+
            "111",

            "101"+
            "101"+
            "111"+
            "001"+
            "001",

            "111"+
            "100"+
            "11"+
            "001"+
            "110",

            "011"+
            "100"+
            "010"+
            "101"+
            "010",

            "111"+
            "001"+
            "010"+
            "100"+
            "100",

            "111"+
            "101"+
            "111"+
            "101"+
            "111",

            "111"+
            "101"+
            "111"+
            "001"+
            "111"
    };

    private final int num;

    public NumberFrame(int num) {
        super(1, 1);
        this.num = Math.min(num, nums.length-1);
    }

    @Override
    public byte getPixel(int x, int y) {
        if(x < 3 && y < 5) {
            if(nums[num].charAt(y*3+x) == '1')
                return (byte) 255;
            else
                return 0;
        }
        else {
            return 0;
        }
    }
};