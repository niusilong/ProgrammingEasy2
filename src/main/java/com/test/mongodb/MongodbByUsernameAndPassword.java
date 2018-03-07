package com.test.mongodb;

import java.util.Arrays;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

public class MongodbByUsernameAndPassword {
	public static void main(String[] args) {
		MongoCredential credential = MongoCredential.createCredential("mongodb", "admin", "123456".toCharArray());
        MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017), Arrays.asList(credential));
        System.out.println("Connect to database successfully");
        DB database = mongoClient.getDB("admin");//获取数据库
        DBCollection collection = database.getCollection("test1");//集合名
        DBObject object = new BasicDBObject();     
        object.put("a", "2");
        object.put("b", "3");
        collection.save(object);
//        DBObject dbObject = new BasicDBObject();
//        db.createCollection("test1", dbObject);
        //查询所有的数据
        DBCursor cur = collection.find();    
        while (cur.hasNext()) {
            System.out.println("while="+cur.next());
        }
        System.out.println("count="+cur.count());
        System.out.println("CursorId="+cur.getCursorId());
        System.out.println("cur="+JSON.serialize(cur));
	}
}
