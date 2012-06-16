#!/bin/bash

for i in `seq 100 1100`;
do
	echo $i;
	wget http://placekitten.com/$i/$i -O $i.jpg
	convert $i.jpg -resize 72x72 avatar_$i.jpg
done
