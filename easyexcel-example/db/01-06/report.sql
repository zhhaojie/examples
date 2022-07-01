-- 各业务线短信服务月度使用量对比
select x.product_line, x.item_day, sum(x.cnt)
from db_report.data_report x
where x.service_name = 'sms'
group by x.product_line, x.item_day
order by x.product_line, x.item_day asc;


-- 各业务线上个月短信使用量
select x.product_line, sum(x.cnt)
from db_report.data_report x
where x.service_name = 'sms'
  and x.item_day = DATE_FORMAT(date_add(now(), interval -1 month),'%Y%m')
group by x.product_line
order by sum(x.cnt) desc;

-- 上个月短信分类使用量
select x.product, sum(x.cnt)
from db_report.data_report x
where x.service_name = 'sms'
  and x.item_day = DATE_FORMAT(date_add(now(), interval -1 month),'%Y%m')
group by x.product
order by sum(x.cnt) desc;


-- 各业务线图像认证服务月度使用量对比
select x.product_line, x.item_day, sum(x.cnt)
from db_report.data_report x
where x.service_name = 'graphic'
group by x.product_line, x.item_day
order by x.product_line, x.item_day asc;


-- 各业务线上个月图像认证使用量
select x.product_line, sum(x.cnt)
from db_report.data_report x
where x.service_name = 'graphic'
  and x.item_day = DATE_FORMAT(date_add(now(), interval -1 month),'%Y%m')
group by x.product_line
order by sum(x.cnt) desc;

-- 上个月图像认证第三方通道使用量
select x.product,sum(x.cnt)
from db_report.data_report x
where x.service_name = 'graphic'
  and x.item_day = DATE_FORMAT(date_add(now(), interval -1 month),'%Y%m')
group by x.product
order by sum(x.cnt) desc;


