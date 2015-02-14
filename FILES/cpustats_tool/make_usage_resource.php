<?php

$template = <<<EOS
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
	<item android:drawable="@drawable/bg" />
	<item android:left="@dimen/dual_left">
	    <bitmap android:src="@drawable/d{CORE1}" android:gravity="left" />
	</item>
	<item android:right="@dimen/dual_right">
	    <bitmap android:src="@drawable/d{CORE2}" android:gravity="right" />
	</item>
</layer-list>
EOS;

$template = <<<EOS
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
	<item android:drawable="@drawable/bg" />
	<item android:left="@dimen/quad_left1">
	    <bitmap android:src="@drawable/q{CORE1}" android:gravity="left" />
	</item>
	<item android:left="@dimen/quad_left2">
	    <bitmap android:src="@drawable/q{CORE2}" android:gravity="left" />
	</item>
	<item android:left="@dimen/quad_left3">
	    <bitmap android:src="@drawable/q{CORE3}" android:gravity="left" />
	</item>
	<item android:left="@dimen/quad_left4">
	    <bitmap android:src="@drawable/q{CORE4}" android:gravity="left" />
	</item>
</layer-list>
EOS;

#echo $template;

$variation = array(0, 1, 3, 5);
$variation = array(0, 1, 2, 3, 4, 5);

foreach ($variation as $c1) {
    foreach ($variation as $c2) {
        foreach ($variation as $c3) {
            foreach ($variation as $c4) {

                $v = $template;
                $v = str_replace('{CORE1}', $c1, $v);
                $v = str_replace('{CORE2}', $c2, $v);
                $v = str_replace('{CORE3}', $c3, $v);
                $v = str_replace('{CORE4}', $c4, $v);
                echo $v;

                file_put_contents('quad_' . $c1 . $c2 . $c3 . $c4 . '.xml', $v);

#                $x = 1000*$c1 + 100*$c2 + 10*$c3 + 1*$c4;
#                printf("case %s: return R.drawable.quad_%d%d%d%d;\n", $x, $c1, $c2, $c3, $c4);
            }
        }

    }
}

?>
