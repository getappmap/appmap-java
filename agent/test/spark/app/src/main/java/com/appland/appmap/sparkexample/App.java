package com.appland.appmap.sparkexample;

import spark.Request;
import spark.Response;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.port;

import org.eclipse.jetty.http.HttpStatus;

public class App {

    Object index(Request req, Response res) {
        return "Hello World!";
    }

    Object error(Request req, Response res) throws Exception {
        throw new Exception("oops");
    }

    Object exit(Request req, Response res) {
        System.exit(0);
        return null;
    }

    Object rest(Request req, Response res) {
        res.status(HttpStatus.NOT_FOUND_404);
        return "";
    }

    public static void main(String[] args) {
        port(8080);

        App app = new App();

        get("/", app::index);
        get("/error", app::error);
        delete("/exit", app::exit);

        get("*", app::rest);
    }
}
