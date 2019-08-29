#!/bin/bash
#if [ -e content.txt ]
#then
#	rm content.txt
#fi

#destFile=../../content.txt

if [ -e content.html ]
then
	rm content.html
fi

destFile=../../content.html

echo \<html\>\<head\>\<title\>Gradle/Groovy\</title\>\</head\>\<body style=\"padding:4em\"\> >> content.html
#echo \<a href=\"#1.01-Exercise-RunYourFirstTask\"\>1.01-Exercise-RunYourFirstTask\</a\>\<br\> >> content.html


cd GRADLE


for file in $(ls)
do

if [ -d $file ]
then
	cd $file
	if [ -e build.gradle ]
	then
		echo \<a href=\"#$file\"\>$file\</a\>\<br\> >> $destFile
		echo \&nbsp\;\&nbsp\;\&nbsp\;\&nbsp\;\<a href=\"#$file\_build.gradle\"\>build.gradle\</a\>\<br\> >> $destFile
	fi
	if [ -e solution.gradle ]
	then
		echo \&nbsp\;\&nbsp\;\&nbsp\;\&nbsp\;\<a href=\"#$file\_solution.gradle\"\>solution.gradle\</a\>\<br\> >> $destFile
	fi
	if [ -e exercise.txt ]
	then
		if [ ! -e build.gradle ]
		then
			echo \<a href=\"#$file\"\>$file\</a\>\<br\> >> $destFile
		fi
		echo \&nbsp\;\&nbsp\;\&nbsp\;\&nbsp\;\<a href=\"#$file\_exercise.txt\"\>exercise.txt\</a\>\<br\> >> $destFile
	fi
  	echo \<br\> >> $destFile
	cd ..
fi

done

echo \<br\>\<br\> >> $destFile



for file in $(ls)
do

if [ -d $file ]
then
	cd $file
	if [ -e build.gradle ]
	then
		text=$(<build.gradle)
		echo \<br\> >> $destFile
		echo \<h1 id=\"$file\"\>$file\</h1\> >> $destFile
		echo \<br\> >> $destFile
		echo \<span id=\"$file\_build.gradle\" style=\"color:green\"\>build.gradle\</span\> \<br\> >> $destFile
		echo \<pre\>$text\</pre\> >> $destFile
		echo \<br\> >> $destFile
		
	fi
	if [ -e solution.gradle ]
	then
		text=$(<solution.gradle)
                
		echo \<br\> >> $destFile
		echo \<span id=\"$file\_solution.gradle\" style=\"color:orange\"\>solution.gradle\</span\> \<br\> >> $destFile
		echo \<pre\>$text\</pre\> \<br\> >> $destFile
		echo \<br\> >> $destFile

	fi
	if [ -e exercise.txt ]
	then
		text=$(<exercise.txt)
                
		echo \<br\> >> $destFile
		echo \<span id=\"$file\_exercise.txt\" style=\"color:blue\"\>exercise.txt\</span\> \<br\> >> $destFile
		echo \<pre\>$text\</pre\> \<br\> >> $destFile
		echo \<br\> >> $destFile

	fi
	cd ..

fi

done

echo \</body\> >> $destFile
echo \</html\> >> $destFile
