LOAD DATA INFILE 'D:/lang/Lift/jinrou_domi/icon_list.txt'
INTO TABLE jinrou_domi.usericon
CHARACTER SET UTF8
FIELDS TERMINATED BY ',' ENCLOSED BY "'"
LINES TERMINATED BY '\r\n'
(icon_name,icon_filename,icon_width,icon_height,color)
SET icon_group = '1',icon_gname = '', created = CURRENT_TIMESTAMP;
