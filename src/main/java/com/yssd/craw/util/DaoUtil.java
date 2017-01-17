package com.yssd.craw.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nutz.dao.Chain;
import org.nutz.dao.Condition;
import org.nutz.dao.Sqls;
import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.MappingField;
import org.nutz.dao.entity.PkType;
import org.nutz.dao.entity.Record;
import org.nutz.dao.impl.NutDao;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.sql.SqlCallback;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.lang.Lang;

import com.alibaba.druid.pool.DruidDataSource;

@SuppressWarnings("unchecked")
public class DaoUtil {
	private static PropertiesProxy prop = new PropertiesProxy("config.properties");  
    private static DaoUtil daoUtil;

    private DaoUtil() {
	init();
    }

    public static DaoUtil newInstance() {
	if (daoUtil == null) {
           daoUtil = new DaoUtil();
	}
	return daoUtil;
    }

    private static NutDao dao = null;

    public void init() {
	String driver = prop.get("db.craw.driver");
	String url = prop.get("db.craw.url");
	String user = prop.get("db.craw.user");
	String password = prop.get("db.craw.password");
	DruidDataSource ds = new DruidDataSource();
	ds.setDriverClassName(driver);
	ds.setUrl(url);
	ds.setUsername(user);
	ds.setPassword(password);
	ds.setMaxActive(2);// 只维护一个链接
//	try {
//	    Connection conn = ds.getConnection();
//	    PreparedStatement pst = conn.prepareStatement("");
//	    
//	} catch (SQLException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//	}
	dao = new NutDao();
	dao.setDataSource(ds);
	
    }

    public List<Record> queryBysql(String sqlStr) {
	Sql sql = Sqls.create(sqlStr);
	dao.execute(sql.setCallback(new SqlCallback() {
	    public List<Record> invoke(Connection conn, ResultSet rs, Sql sql) throws SQLException {
		List<Record> result = new ArrayList<Record>();
		while (rs.next()) {
		    result.add(Record.create(rs));
		}
		return result;
	    }
	}));
	return (List<Record>) sql.getResult();
    }
    public <T> T find(int id,Class<T> c){
		return dao.fetch(c, id);
	}
    public Record queryOneBysql(String sqlStr) {
	List<Record> select = queryBysql(sqlStr);
	if (select != null && select.size() > 0) {
	    return select.get(0);
	} else {
	    return null;
	}
    }

    public <T> List<T> search(Class<T> c, Condition condition) {
	return dao.query(c, condition, null);
    }
    
    public <T> T insertOrUpdate(T obj) {
	    if (obj == null)
	      return null;
	    Entity<T> en = (Entity<T>) dao.getEntity(obj.getClass());
	    if (en.getPkType() == PkType.UNKNOWN)
	      throw new IllegalArgumentException("no support , without pks");
	    boolean doInsert = false;
	    switch (en.getPkType()) {
	      case ID:
	        Number n = (Number) en.getIdField().getValue(obj);
	        if (n == null || n.intValue() == 0)
	          doInsert = true;
	        break;
	      case NAME:
	        if (null == en.getNameField().getValue(obj))
	          doInsert = false;
	        break;
	      case COMPOSITE:
	        doInsert = true;
	        for (MappingField mf : en.getCompositePKFields()) {
	          Object v = mf.getValue(obj);
	          if (v != null) {
	            if (v instanceof Number && ((Number) v).intValue() != 0) {
	              continue;
	            }
	            doInsert = true;
	          }
	        }
	      case UNKNOWN:
	        throw Lang.impossible();
	    }
	    if (doInsert) {
	      return this.save(obj);
	    } else {
	      this.update(obj);
	      return obj;
	    }
	  }

    public <T> T fetchByCondition(Class<T> c, Condition condition) {
	return dao.fetch(c, condition);
    }
    

    public <T> T save(T t) {
	T t1 = dao.insert(t);
	return t1;
    }

    public <T> boolean update(Class<T> c,Chain chain,Condition condition){
	return dao.update(c, chain, condition) > 0;
}
    public <T> boolean update(T t) {
	dao.update(t);
	return true;
    }
    public <T> T searchOne( Class<T> clz,Condition condition) {
		List<T> list = search(clz, condition);
		if(list == null || list.size() == 0){
			return null;
		}
		return list.get(0);
	}
    public void executeSql(String sqlStr) {
	Sql sql = Sqls.create(sqlStr);
	dao.execute(sql);
    }

    public static void main(String[] args) {
		DaoUtil.newInstance().queryBysql("select 1");
	}
}