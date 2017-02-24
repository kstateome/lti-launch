<?xml version="1.0" encoding="UTF-8" ?>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <title>Unknown Error</title>
</head>
<body>
  <h2>Unknown Error</h2>
  <p>There was an unexpected error while processing your request. Please try reloading
     this LTI application by clicking your browser's refresh button.
     If this problem persists, contact the help desk.
  </p>
  <p>When reporting an error, please include the following information to help us track it down:</p>
  <ul>
    <li>Time of error</li>
    <li>Canvas course ID</li>
    <li>Your user ID</li>
    <li>The error message displayed below (if any)</li>
  </ul>
  
  <c:if test="${not empty errorMessage}">
    <h3>Error message:</h3>
    <p>${errorMessage}</p>
  </c:if>
</body>
</html>