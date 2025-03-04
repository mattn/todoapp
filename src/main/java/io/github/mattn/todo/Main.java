package io.github.mattn.todo;

import io.github.mattn.todo.models.Item;
import io.javalin.Javalin;
import io.javalin.vue.VueComponent;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Main.
 */
public class Main {
    /**
     * Main function.
     * 
     * @param args arguments for program.
     * @throws Exception throws when something wrong.
     */
    public static void main(String[] args) throws Exception {
        SqlSessionFactory factory;
        try (InputStream in = Main.class.getResourceAsStream("/mybatis-config.xml")) {
            factory = new SqlSessionFactoryBuilder().build(in);
        }
        try (SqlSession session = factory.openSession()) {
            session.update("io.github.mattn.todo.create");
            session.commit();
        }

        Javalin app = Javalin.create(config -> {
            config.staticFiles.enableWebjars();
            config.vue.isDevFunction = ctx -> false;
            config.vue.vueInstanceNameInJs = "app";
        }).start(7000);

        app.get("/api/todo", ctx -> {
            ctx.res().setContentType("text/json");

            try (SqlSession session = factory.openSession()) {
                List<Item> result = session.selectList("io.github.mattn.todo.select");
                ctx.json(result);
            }
        });
        app.post("/api/todo", ctx -> {
            Item item = ctx.bodyAsClass(Item.class);
            try (SqlSession session = factory.openSession()) {
                session.insert("io.github.mattn.todo.insert", item);
                session.commit();
                ctx.status(201);
            }
        });
        app.post("/api/todo/{id}", ctx -> {
            Item item = ctx.bodyAsClass(Item.class);
            item.setUpdatedAt(new Date());
            try (SqlSession session = factory.openSession()) {
                session.insert("io.github.mattn.todo.update", item);
                session.commit();
            }
        });
        app.delete("/api/todo/{id}", ctx -> {
            try (SqlSession session = factory.openSession()) {
                session.delete("io.github.mattn.todo.delete", Integer.parseInt(ctx.pathParam("id")));
                session.commit();
            }
        });
        app.get("/", new VueComponent("<todo-overview></todo-overview>"));
    }
}
