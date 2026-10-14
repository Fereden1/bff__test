<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8"/>
  <title>Самокаты</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css"/>
</head>
<body data-ctx="${pageContext.request.contextPath}">
  <h2>Ближайшие самокаты</h2>
  <p><a href="logout">Выйти</a>
    <button type="button" id="btn-refresh">Обновить</button>
  </p>
  <p id="coords" class="muted"></p>
  <div id="list">Загрузка...</div>
  <div id="active-ride"></div>
  <h3>Мои поездки</h3>
  <pre id="rides"></pre>

  <script src="${pageContext.request.contextPath}/js/geo.js"></script>
  <script src="${pageContext.request.contextPath}/js/main.js"></script>
</body>
</html>
