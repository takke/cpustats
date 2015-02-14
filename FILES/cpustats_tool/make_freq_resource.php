<?php

$template = <<<EOS
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
	<item android:drawable="@drawable/freq_bg" />
	<item android:left="@dimen/freq_left1" android:top="@dimen/freq_top">
	    <bitmap android:src="@drawable/digit{DIGIT_A}" android:gravity="left" />
	</item>
	<item android:left="@dimen/freq_left2" android:top="@dimen/freq_top">
	    <bitmap android:src="@drawable/dot" android:gravity="left" />
	</item>
	<item android:left="@dimen/freq_left3" android:top="@dimen/freq_top">
	    <bitmap android:src="@drawable/digit{DIGIT_B}" android:gravity="left" />
	</item>
</layer-list>
EOS;

#echo $template;

for ($i=1; $i<=50; $i++) {
    $a = (int)($i / 10);
    $b = (int)($i % 10);

    $v = $template;
    $v = str_replace('{DIGIT_A}', $a, $v);
    $v = str_replace('{DIGIT_B}', $b, $v);
#    echo $v;
#    printf("%s.%s\n", $a, $b);

    file_put_contents('freq_' . $a . $b . '.xml', $v);

#   $x = 1000*$c1 + 100*$c2 + 10*$c3 + 1*$c4;
#   printf("case %s: return R.drawable.freq_%d%d;\n", $i, $a, $b);
}

?>
