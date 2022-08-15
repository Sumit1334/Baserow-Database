package com.sumit1334.baserow;

import android.app.Activity;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.YailList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Baserow extends AndroidNonvisibleComponent implements Component {
    private final String TAG = "Baserow";
    private final Activity activity;
    private final Request request;
    private final String mainBaseUrl = "/api/database/rows/table/";
    private String token;
    private String table;
    private String baseUrl = "https://api.baserow.io";
    private String JWT;

    public Baserow(ComponentContainer container) {
        super(container.$form());
        this.activity = container.$context();
        this.request = new Request();
        Log.i(TAG, "Extension Initialized");
    }

    @SimpleProperty
    @DesignerProperty(defaultValue = "https://api.baserow.io", editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    public void URL(String url) {
        this.baseUrl = url;
    }

    @SimpleProperty
    public String URL() {
        return this.baseUrl;
    }

    @SimpleProperty
    @DesignerProperty()
    public void TableID(String tableName) {
        this.table = tableName;
    }

    @SimpleProperty
    public String TableID() {
        return this.table;
    }

    @SimpleProperty
    @DesignerProperty()
    public void Token(String token) {
        this.token = token;
    }

    @SimpleProperty
    public String Token() {
        return this.token;
    }

    @SimpleEvent
    public void GotRow(YailList values) {
        EventDispatcher.dispatchEvent(this, "GotRow", values);
    }

    @SimpleEvent
    public void GotAllRows(String responseContent, int totalRows) {
        EventDispatcher.dispatchEvent(this, "GotAllRows", responseContent, totalRows);
    }

    @SimpleEvent
    public void RowCreated(String response) {
        EventDispatcher.dispatchEvent(this, "RowCreated", response);
    }

    @SimpleEvent
    public void RowMoved(String response) {
        EventDispatcher.dispatchEvent(this, "RowMoved", response);
    }

    @SimpleEvent
    public void RowUpdated(String response) {
        EventDispatcher.dispatchEvent(this, "RowUpdated", response);
    }

    @SimpleEvent
    public void RowDeleted(String response) {
        EventDispatcher.dispatchEvent(this, "RowDeleted", response);
    }

    @SimpleEvent
    public void GotColumnsNames(YailList names, YailList fields) {
        EventDispatcher.dispatchEvent(this, "GotColumnsNames", names, fields);
    }

    @SimpleEvent
    public void ErrorOccurred(int responseCode, String error) {
        Log.e(TAG, error);
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", responseCode, error);
    }

    @SimpleEvent
    public void GotColumn(String response, YailList values) {
        EventDispatcher.dispatchEvent(this, "GotColumn", response, values);
    }

    @SimpleEvent
    public void TokenGenerated(final String response, final String token) {
        EventDispatcher.dispatchEvent(this, "TokenGenerated", response, token);
    }

    @SimpleEvent
    public void FileUploaded(String response, String url, String fileName) {
        EventDispatcher.dispatchEvent(this, "FileUploaded", response, url, fileName);
    }

    @SimpleEvent
    public void TokenValidated(boolean isValid, String token) {
        EventDispatcher.dispatchEvent(this, "TokenValidated", isValid, token);
    }

    @SimpleFunction
    public void UploadFileByUrl(String token, String url) {
        if (url.isEmpty() || token.isEmpty())
            throw new IllegalArgumentException("url cant be empty");
        else {
            this.JWT = token;
            String urlPath = this.baseUrl + "/api/user-files/upload-via-url/";
            String data = this.makeJson(new String[]{"url"}, new String[]{url});
            this.request.SendData(urlPath, "POST", true, data, new Callback() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject object = new JSONObject(result);
                                String file = object.getString("url");
                                String nam = object.getString("name");
                                FileUploaded(result, file, nam);
                            } catch (Exception e) {
                                ErrorOccurred(0, e.getMessage());
                            }
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction
    public void UploadFile(String token, String path) {
        if (path.isEmpty() || token.isEmpty())
            throw new IllegalArgumentException("parameters cant be empty");
        else {
            this.JWT = token;
            this.request.UploadFile(path, new Callback() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject object = new JSONObject(result);
                                String file = object.getString("url");
                                String nam = object.getString("original_name");
                                FileUploaded(result, file, nam);
                            } catch (Exception e) {
                                ErrorOccurred(0, e.getMessage());
                            }
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction(description = "This block is not recommended to be used in public apps. So use this block in your admin side")
    public void GenerateToken(final String email, final String password) {
        if (email.isEmpty() || password.isEmpty())
            throw new IllegalArgumentException("Given parameters are empty");
        else {
            String url = this.baseUrl + "/api/user/token-auth/";
            String data = this.makeJson(new String[]{"username", "password"}, new String[]{email, password});
            this.request.SendData(url, "POST", false, data, new Callback() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String tokenGenerated = "";
                            try {
                                JSONObject object = new JSONObject(result);
                                tokenGenerated = object.getString("token");
                            } catch (Exception e) {
                                ErrorOccurred(0, e.getMessage());
                            }
                            TokenGenerated(result, tokenGenerated);
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction
    public void RefreshToken(final String token) {
        if (token.isEmpty())
            throw new IllegalArgumentException("Given parameters are empty");
        else {
            String url = this.baseUrl + "/api/user/token-refresh/";
            String data = this.makeJson(new String[]{"token"}, new String[]{token});
            this.request.SendData(url, "POST", false, data, new Callback() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String tokenGenerated = "";
                            try {
                                JSONObject object = new JSONObject(result);
                                tokenGenerated = object.getString("token");
                            } catch (Exception e) {
                                ErrorOccurred(0, e.getMessage());
                            }
                            TokenGenerated(result, tokenGenerated);
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction
    public void ValidateToken(final String token) {
        if (token.isEmpty())
            throw new IllegalArgumentException("Given parameters are empty");
        else {
            String url = this.baseUrl + "/api/user/token-verify/";
            String data = this.makeJson(new String[]{"token"}, new String[]{token});
            this.request.SendData(url, "POST", false, data, new Callback() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String tokenGenerated = "";
                            try {
                                JSONObject object = new JSONObject(result);
                                tokenGenerated = object.getString("token");
                            } catch (Exception e) {
                                ErrorOccurred(0, e.getMessage());
                            }
                            TokenValidated(tokenGenerated.equals(token), tokenGenerated);
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction
    public void MoveRow(int rowId, int beforeRowId) {
        if (rowId > 0 && beforeRowId > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append(this.baseUrl).append(this.mainBaseUrl);
            sb.append(table);
            sb.append("/");
            sb.append(rowId);
            sb.append("/move/?before_id=");
            sb.append(beforeRowId);
            this.request.MakeGetRequest(sb.toString(), "PATCH", new Result() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            RowMoved(result);
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction
    public void DeleteRow(int rowId) {
        if (rowId > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append(this.baseUrl);
            sb.append(this.mainBaseUrl);
            sb.append(table);
            sb.append("/");
            sb.append(rowId);
            sb.append("/");
            this.request.MakeGetRequest(sb.toString(), "DELETE", new Result() {
                @Override
                public void Success(final String result) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                final JSONObject response = new JSONObject();
                                response.put("row_id", rowId);
                                RowDeleted(response.toString());
                            } catch (Exception e) {
                                ErrorOccurred(0, e.getMessage());
                            }
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction
    public void GetColumnsNames() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl);
        final String mainFieldUrl = "/api/database/fields/table/";
        sb.append(mainFieldUrl).append(table).append("/");
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            ArrayList<String> fields = new ArrayList<>();
                            ArrayList<String> list = new ArrayList<>();
                            JSONArray jsonArray = new JSONArray(result);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                list.add(object.getString("name"));
                                fields.add("field_" + object.getString("id"));
                            }
                            GotColumnsNames(YailList.makeList(list), YailList.makeList(fields));
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void GetAllRows(int page, int maxRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl);
        sb.append(this.mainBaseUrl);
        sb.append(this.table);
        sb.append("/?user_field_names=true&size=");
        sb.append(maxRecord);
        sb.append("&page=");
        sb.append(page);
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            int total = 0;
                            JSONObject object = new JSONObject(result);
                            total = object.getInt("count");
                            GotAllRows(result, total);
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void Search(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl);
        sb.append(this.mainBaseUrl);
        sb.append(this.table);
        sb.append("/?user_field_names=true&search=");
        sb.append(text);
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            JSONObject object = new JSONObject(result);
                            GotAllRows(result, object.getInt("count"));
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void Order(String columnName, int page, String technique, int maxRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl);
        sb.append(this.mainBaseUrl);
        sb.append(this.table);
        sb.append("/?user_field_names=true&order_by=");
        sb.append(technique.equals(Ascending()) ? columnName : "-" + columnName);
        sb.append("&size=");
        sb.append(maxRecord);
        sb.append("&page=");
        sb.append(page);
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    final String readLine = result;

                    public void run() {
                        try {
                            final JSONObject object = new JSONObject(readLine);
                            GotAllRows(readLine, object.getInt("count"));
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void GetColumn(YailList columnNames, int page, int maxRecord) {
        String columns = "";
        for (int i = 0; i < columnNames.toStringArray().length; i++) {
            String item = columnNames.toStringArray()[i];
            columns = columns + (i == 0 ? item : "," + item);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl);
        sb.append(this.mainBaseUrl);
        sb.append(this.table);
        sb.append("/?user_field_names=true&include=");
        sb.append(columns);
        sb.append("&size=");
        sb.append(maxRecord);
        sb.append("&page=");
        sb.append(page);
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            ArrayList<YailList> allList = new ArrayList<>();
                            for (String columnName : columnNames.toStringArray()) {
                                allList.add(YailList.makeList(getList(result, columnName)));
                            }
                            GotColumn(result, YailList.makeList(allList));
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void GetRow(int id) {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl)
                .append(this.mainBaseUrl)
                .append(this.table)
                .append("/")
                .append(id)
                .append("/?user_field_names=true");
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            final JSONObject jsonObject = new JSONObject(result);
                            ArrayList<String> list = new ArrayList<>();
                            Iterator<String> iterator = jsonObject.keys();
                            while (iterator.hasNext()) {
                                String key = iterator.next().toString();
                                String value = null;
                                if (!(key.equals("id") || key.equals("order"))) {
                                    try {
                                        value = jsonObject.getString(key);
                                    } catch (JSONException e) {
                                        ErrorOccurred(0, e.getMessage());
                                    }
                                    list.add(value);
                                }
                            }
                            GotRow(YailList.makeList(list));
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @SimpleFunction
    public void CreateRow(YailList columnNames, YailList values) {
        if (columnNames.length() == values.length()) {
            String[] names = columnNames.toStringArray();
            String[] value = values.toStringArray();
            StringBuilder sb = new StringBuilder();
            sb.append(this.baseUrl);
            sb.append(this.mainBaseUrl);
            sb.append(this.table);
            sb.append("/?user_field_names=true");
            String data = this.makeJson(names, value);
            this.request.PostData(sb.toString(), data, "POST", new Result() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            RowCreated(result);
                        }
                    });
                }
            });

        }
    }

    @SimpleFunction
    public void UpdateRow(int rowId, String name, String value) {
        if (name != null && value != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.baseUrl);
            sb.append(this.mainBaseUrl);
            sb.append(this.table);
            sb.append("/");
            sb.append(rowId);
            sb.append("/?user_field_names=true");
            String data = this.makeJson(new String[]{name}, new String[]{value});
            this.request.PostData(sb.toString(), data, "PATCH", new Result() {
                @Override
                public void Success(String result) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            RowUpdated(result);
                        }
                    });
                }
            });
        }
    }

    @SimpleFunction(description = "Update a row with multiple column values")
    public void UpdateRows(int rowId, YailList columnsNames, YailList values) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl)
                .append(this.mainBaseUrl)
                .append(this.table)
                .append("/")
                .append(rowId)
                .append("/?user_field_names=true");
        String data = this.makeJson(columnsNames.toStringArray(), values.toStringArray());
        this.request.PostData(sb.toString(), data, "PATCH", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        RowUpdated(result);
                    }
                });
            }
        });
    }

    @SimpleFunction
    public YailList List(String response, String columnName) {
        return YailList.makeList(this.getList(response, columnName));
    }

    @SimpleFunction(description = "Converts the given list of keys and values to a JSON string")
    public String MakeJSON(YailList names, YailList values) {
        return makeJson(names.toStringArray(), values.toStringArray());
    }

    @SimpleFunction
    public void Filter(String fieldId, String filter, String value, int page, int maxRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUrl);
        sb.append(this.mainBaseUrl);
        sb.append(this.table);
        sb.append("/?user_field_names=true&size=");
        sb.append(maxRecord);
        sb.append("&page=");
        sb.append(page);
        sb.append("&filter__");
        sb.append(fieldId);
        sb.append(filter);
        sb.append(filter.equals(Empty()) || filter.equals(NotEmpty()) ? "" : value);
        this.request.MakeGetRequest(sb.toString(), "GET", new Result() {
            @Override
            public void Success(String result) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            int total = 0;
                            JSONObject object = new JSONObject(result);
                            total = object.getInt("count");
                            GotAllRows(result, total);
                        } catch (Exception e) {
                            ErrorOccurred(0, e.getMessage());
                        }
                    }
                });
            }
        });
    }

    private String makeJson(String[] names, String[] values) {
        YailDictionary dictionary = new YailDictionary();
        for (int i = 0; i < names.length; i++) {
            dictionary.put(names[i], values[i]);
        }
        return dictionary.toString();
    }

    private ArrayList<String> getList(String response, String name) {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONObject object = new JSONObject(response);
            JSONArray array = object.getJSONArray("results");
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getJSONObject(i).getString(name));
            }
            return list;
        } catch (Exception e) {
            ErrorOccurred(0, e.getMessage());
            return list;
        }
    }

    @SimpleProperty
    public String Ascending() {
        return "Ascending";
    }

    @SimpleProperty
    public String Descending() {
        return "Descending";
    }

    @SimpleProperty
    public String Equal() {
        return "__equal=";
    }

    @SimpleProperty
    public String NotEqual() {
        return "__not_equal=";
    }

    @SimpleProperty
    public String Contain() {
        return "__contains=";
    }

    @SimpleProperty
    public String NotContain() {
        return "__contains_not=";
    }

    @SimpleProperty
    public String Higher() {
        return "__higher_than=";
    }

    @SimpleProperty
    public String Lower() {
        return "__lower_than=";
    }

    @SimpleProperty
    public String Empty() {
        return "__empty";
    }

    @SimpleProperty
    public String NotEmpty() {
        return "__not_empty";
    }

    interface Callback {
        void Success(final String result);
    }

    interface Result {
        void Success(final String result);
    }

    class Request {
        public void PostData(final String uri, final String data, final String method, final Result result) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(uri.replaceAll("//", "/"));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.addRequestProperty("Authorization", "Token " + token);
                        connection.addRequestProperty("Content-Type", "application/json");
                        connection.setRequestMethod(method);
                        connection.setDoOutput(true);
                        OutputStream os = connection.getOutputStream();
                        os.write(data.getBytes(StandardCharsets.UTF_8));
                        int responseCode = connection.getResponseCode();
                        if (responseCode / 100 == 2) {
                            InputStream inputStream = connection.getInputStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder stringBuilder = new StringBuilder();
                            int cp;
                            while ((cp = bufferedReader.read()) != -1) {
                                stringBuilder.append((char) cp);
                            }
                            final String readLine = stringBuilder.toString();
                            result.Success(readLine);
                        } else {
                            InputStream inputStream = connection.getErrorStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder stringBuilder = new StringBuilder();
                            int cp;
                            while ((cp = bufferedReader.read()) != -1) {
                                stringBuilder.append((char) cp);
                            }
                            final String readLine = stringBuilder.toString();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ErrorOccurred(responseCode, readLine);
                                }
                            });
                        }
                    } catch (Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ErrorOccurred(0, e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        }

        public void MakeGetRequest(final String uri, final String method, final Result result) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(uri.replaceAll("//", "/"));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod(method);
                        connection.addRequestProperty("Authorization", "Token " + token);
                        int responseCode = connection.getResponseCode();
                        if (responseCode / 100 == 2) {
                            InputStream inputStream = connection.getInputStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                            final String readLine = bufferedReader.readLine();
                            connection.disconnect();
                            bufferedReader.close();
                            result.Success(readLine);
                        } else {
                            InputStream inputStream = connection.getErrorStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                            final String readLine = bufferedReader.readLine();
                            connection.disconnect();
                            bufferedReader.close();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ErrorOccurred(responseCode, readLine);
                                }
                            });
                        }
                    } catch (Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ErrorOccurred(0, e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        }

        public void SendData(String url, String method, final boolean auth, String data, Callback callback) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        connection.setConnectTimeout(5000);
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoInput(true);
                        connection.setDoOutput(true);
                        connection.setRequestMethod(method);
                        if (auth)
                            connection.addRequestProperty("Authorization", "JWT " + JWT);
                        OutputStream os = connection.getOutputStream();
                        os.write(data.getBytes(StandardCharsets.UTF_8));
                        int responseCode = connection.getResponseCode();
                        if (responseCode / 100 == 2) {
                            InputStream inputStream = connection.getInputStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder stringBuilder = new StringBuilder();
                            int cp;
                            while ((cp = bufferedReader.read()) != -1) {
                                stringBuilder.append((char) cp);
                            }
                            final String readLine = stringBuilder.toString();
                            callback.Success(readLine);
                        } else {
                            InputStream inputStream = connection.getErrorStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder stringBuilder = new StringBuilder();
                            int cp;
                            while ((cp = bufferedReader.read()) != -1) {
                                stringBuilder.append((char) cp);
                            }
                            final String readLine = stringBuilder.toString();
                            ErrorOccurred(responseCode, readLine);
                        }
                    } catch (Exception e) {
                        ErrorOccurred(0, e.getMessage());
                    }
                }
            }).start();
        }

        public void UploadFile(String path, Callback callback) {
            Log.i(TAG, "UploadFile: Uploading file from path = " + path);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String lineEnd = "\r\n";
                        String twoHyphens = "------";
                        String boundary = "WebKitFormBoundary" + generate();
                        int bytesRead, bytesAvailable, bufferSize;
                        byte[] buffer;
                        int maxBufferSize = 1024 * 1024;
                        URL url = new URL(baseUrl + "/api/user-files/upload-file/");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.setDoOutput(true);
                        connection.setUseCaches(false);
                        connection.setRequestMethod("POST");
                        connection.addRequestProperty("Authorization", "JWT " + JWT);
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                        FileInputStream fileInputStream;
                        DataOutputStream outputStream;
                        outputStream = new DataOutputStream(connection.getOutputStream());
                        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                        outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + path.substring(path.lastIndexOf("/") + 1) + "\"" + lineEnd);
                        outputStream.writeBytes("Content-Type: image/" + path.substring(path.lastIndexOf(".") + 1));
                        outputStream.writeBytes(lineEnd);
                        outputStream.writeBytes(lineEnd);
                        fileInputStream = new FileInputStream(path);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        while (bytesRead > 0) {
                            outputStream.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }
                        outputStream.writeBytes(lineEnd);
                        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                        int responseCode = connection.getResponseCode();
                        if (responseCode / 100 == 2) {
                            String result;
                            InputStream is = new BufferedInputStream(connection.getInputStream());
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            result = br.readLine();
                            fileInputStream.close();
                            outputStream.flush();
                            outputStream.close();
                            callback.Success(result == null ? "Uploaded" : result);
                        } else {
                            InputStream is = new BufferedInputStream(connection.getErrorStream());
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            final String result = br.readLine();
                            fileInputStream.close();
                            outputStream.flush();
                            outputStream.close();
                            ErrorOccurred(responseCode, result);
                        }
                    } catch (Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ErrorOccurred(0, e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        }

        private String generate() {
            char[] array = "abcdefghijklmnopqrstuvwxyz".toCharArray();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                builder.append(array[new Random().nextInt(25)]);
            }
            return builder.toString();
        }
    }
}