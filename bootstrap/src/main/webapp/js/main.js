require.config({
  baseUrl: '/node_modules',
  paths: {
  }
});

require(["swagger-client/lib/shred.bundle", "swagger-client/lib/swagger"], function() {
  window.swagger = new SwaggerApi({
    url: "http://localhost:8080/api-docs",
    success: function() {
      if(swagger.ready === true) {
        swagger.apis.categories.getRootCategories(
          function(data) {
            console.log(data);
          },
          function(err) {
            console.log(err);
          }
        );
      }
    }
  })
});

require(["jquery/dist/jquery"])