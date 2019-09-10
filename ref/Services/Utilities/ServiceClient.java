package com.connectfss.MobileBanking.Services.Utilities;

import android.content.Context;
import android.os.Handler;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.connectfss.MobileBanking.R;
import com.connectfss.MobileBanking.Services.Models.StringResource;
import com.connectfss.MobileBanking.Services.Toolbox.BooleanRequest;
import com.connectfss.MobileBanking.Services.Toolbox.JsonArrayRequest;
import com.connectfss.MobileBanking.Services.Toolbox.JsonObjectRequest;
import com.connectfss.MobileBanking.Services.Toolbox.StringRequest;
import com.connectfss.MobileBanking.Utilities.GlobalSettings;
import com.connectfss.MobileBanking.Utilities.JsonHelper;
import com.connectfss.MobileBanking.Utilities.Log;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Web API Services Client
 *
 * Uses Volley for handling network requests, relying on Volley's thread management
 */
public class ServiceClient<T> {

	/**
	 * the constant value for the log tag
	 */
	private static final String LOG_TAG = "SERVICE-CLIENT";

	/**
	 * the constant value of the json node for the Web API error message
	 */
	private static final String WEB_API_MESSAGE_NODE = "message";

	/**
	 * the constant value of the json node for the Web API model state
	 */
	private static final String WEB_API_MODEL_STATE_NODE = "modelState";

	/**
	 * the constant value of the minimum request time in milliseconds
	 */
	private static final int MINIMUM_REQUEST_TIME_MILLIS = 500;

	/**
	 * the request start time millis
	 */
	private long mRequestStartTimeMillis;

	/**
	 * Setup a Volley Request (and the Volley Queue singleton)
	 * then queue-up the volley request
	 *
	 * Note: This is Thread-Safe, Volley will handle everything
	 *
	 * @param context		the context of the application
	 * @param request		the request to queue with the service-client
	 * @param shouldCache	an indicator indicating whether or not to cache the request/response if able
	 * @param fromCache		an indicator indicating whether or not to fetch the response from cache if able
	 */
	@SuppressWarnings("unchecked")
	public void queueRequest(Context context, ServiceRequest request, boolean shouldCache, boolean fromCache) {

		// set the request start time millis
		this.mRequestStartTimeMillis = System.currentTimeMillis();

		// notify the service adapter of the start event
		ServiceAdapter serviceAdapter = request.getServiceAdapter();
		if(serviceAdapter != null) {
			serviceAdapter.onStart();
		}

		// set the web-api request url
		String url = request.getWebApiUrl();

		if(GlobalSettings.DEBUG) {
			Log.d(LOG_TAG, "::REQUEST URL::" + url);
		}

		// setup the Volley request-queue
		ServiceQueue queue = ServiceQueue.getInstance(context);

		// if indicated to fetch from cache and cache exists
		if(fromCache && queue.getCache().get(url) != null) {
			try { // cached response exists, try to process it as a JSONObject
				String json = new String(queue.getCache().get(url).data);
				this.processResponse(request, new JSONObject(json));
			} catch(JSONException e) {
				e.printStackTrace();
				this.processResponseError(context, request, null);
			}
		} else { // not fetching from cache
			// setup the Volley request
			Request volleyRequest = this.getVolleyRequest(context, request);

			// set to cache as indicated
			volleyRequest.setShouldCache(shouldCache);
			volleyRequest.setRetryPolicy(new DefaultRetryPolicy(
					context.getResources().getInteger(R.integer.Config_WebServiceConnectTimeout),
					DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

			if(GlobalSettings.DEBUG) {
				try {
					HashMap<String, String> headers = new HashMap<>(volleyRequest.getHeaders());
					for (Map.Entry<String, String> entry : headers.entrySet()) {
						Log.d(LOG_TAG, "::REQUEST HEADER::" + entry.getKey() + "::" + entry.getValue());
					}
				} catch (AuthFailureError e) {
					Log.e(e);
				}
			}

			// add the request to the queue
			queue.add(volleyRequest);
		}
	}

	/**
	 * Setup and return a specific type of volley-request based upon the request-type
	 *
	 * @param context	the context of the application
	 * @param request	the request to setup the volley-request for
	 * @return			the specific type of volley request setup for the given request
	 */
	@SuppressWarnings("unchecked")
	private Request getVolleyRequest(Context context, ServiceRequest request) {
		// setup Volley request based upon Volley request type
		switch(request.getRequestType()) {

			case BooleanRequest: // Volley Request: Boolean Request
				if(GlobalSettings.DEBUG) {
					Log.d(LOG_TAG, "::REQUEST PAYLOAD::" + JsonHelper.getJsonString(request.getJSONObjectRequestPayload()));
				}
				return new BooleanRequest(this.getVolleyRequestMethod(request.getRequestMethod()),
						request.getWebApiUrl(), request.getJSONObjectRequestPayload().toString(), new ResponseListener(request), new ErrorListener(context, request));

			case StringRequest: // Volley Request: String Request
				if(request.getJSONObjectRequestPayload() != null || request.getJSONArrayRequestPayload() != null) {
					throw new IllegalArgumentException("POST data is not supported by the Volley StringRequest! Switch to using either a JSONArrayRequest or JSONObjectRequest.");
				} else {
					return new StringRequest(this.getVolleyRequestMethod(request.getRequestMethod()),
							request.getWebApiUrl(), new ResponseListener(request), new ErrorListener(context, request));
				}

			case JSONArrayRequest: // Volley Request: JSONArray Request
				if(GlobalSettings.DEBUG) {
					Log.d(LOG_TAG, "::REQUEST PAYLOAD::" + JsonHelper.getJsonString(request.getJSONArrayRequestPayload()));
				}
				return new JsonArrayRequest(this.getVolleyRequestMethod(request.getRequestMethod()),
						request.getWebApiUrl(), request.getJSONArrayRequestPayload(), new ResponseListener(request), new ErrorListener(context, request));

			case JSONObjectRequest: // Volley Request: JSONObject Request
				if(GlobalSettings.DEBUG) {
					Log.d(LOG_TAG, "::REQUEST PAYLOAD::" + JsonHelper.getJsonString(request.getJSONObjectRequestPayload()));
				}
				return new JsonObjectRequest(this.getVolleyRequestMethod(request.getRequestMethod()),
						request.getWebApiUrl(), request.getJSONObjectRequestPayload(), new ResponseListener(request), new ErrorListener(context, request));
		}
		throw new IllegalArgumentException("Invalid Volley request type.");
	}

	/**
	 * Get the Volley request method by converting the ServiceRequest request-method type
	 *
	 * @param requestMethodType the ServiceRequest request-method type to use for conversion
	 * @return 					the Volley request method by converting the ServiceRequest request-method type
	 */
	private int getVolleyRequestMethod(ServiceRequest.RequestMethods requestMethodType) {
		// switch based upon the request-method type
		switch(requestMethodType) {

			case POST: // Volley Request Method: POST
				return Request.Method.POST;

			case PUT: // Volley Request Method: PUT
				return Request.Method.PUT;

			case DELETE: // Volley Request Method: DELETE
				return Request.Method.DELETE;

			case GET: // Volley Request Method: GET
			default:  // Default: GET
				return Request.Method.GET;
		}
	}

	/**
	 * Process the payload object received from the volley-request
	 *
	 * @param payload the payload object to process
	 */
	@SuppressWarnings("unchecked")
	private void processResponse(ServiceRequest serviceRequest, Object payload) {

		// set the service adapter and hand the response payload
		ServiceAdapter serviceAdapter = serviceRequest.getServiceAdapter();
		if(serviceAdapter != null) {

			// set the response payload type
			Class<T> responsePayloadType = serviceRequest.getResponsePayloadType();

			// if JSONObject response
			if (payload instanceof JSONObject) {
				if (responsePayloadType == JSONObject.class) {

					// pass generic JSON object on to the listener
					serviceAdapter.onSuccess(payload);

				} else {

					// prepare a reply from the response and pass on to the listener
					// (converted JSONObject to ResponsePayloadType)
					Gson gson = new GsonBuilder().create();
					serviceAdapter.onSuccess(gson.fromJson(payload.toString(), responsePayloadType));
				}

			} else if (payload instanceof JSONArray) {
				// prepare a reply from the response and pass on to the listener
				// (converted JSONArray to a List of JSONObjects of type ResponsePayloadType)
				Type listType = new TypeToken<List<T>>() {}.where(new TypeParameter<T>() {}, responsePayloadType).getType();
				List<T> payloadList = new Gson().fromJson(payload.toString(), listType);
				serviceAdapter.onSuccess(payloadList);

			} else if (payload instanceof String && responsePayloadType == Boolean.class) {
				// handle the conversion from string to boolean since the expected response is boolean
				serviceAdapter.onSuccess(Boolean.valueOf(payload.toString()));

			} else if (payload == null && responsePayloadType == Integer.class) {
				// since the payload is null and the request was successful,
				// simply return the status code of 200 as expected
				serviceAdapter.onSuccess(200);

			} else { // response could be anything, let the specific service handle it
				serviceAdapter.onSuccess(payload);
			}

			// all finished, notify the service adapter of completion
			serviceAdapter.onComplete();
		}
	}

	/**
	 * Process the volley-error received from the volley-request
	 *
	 * @param context	the context of the application
	 * @param error		the volley-error to process
	 */
	private void processResponseError(Context context, ServiceRequest serviceRequest, VolleyError error) {

		// print the volley stacktrace if possible
		if(error != null) { error.printStackTrace(); }

		// setup a ServiceError object to pass through to the listener
		ServiceError serviceError = new ServiceError();

		// set the initial generic error message
		serviceError.ErrorMessage = StringResource.CommonError_UnknownError.getResourceValue();

		// let the generic error message be overridden by specific known situations
		if(error != null && error.networkResponse != null) {
			try { // expect the response data as a JSONArray string
				JSONObject json = new JSONObject(new String(error.networkResponse.data));
				if(json.has(ServiceClient.WEB_API_MESSAGE_NODE)) {
					serviceError.ErrorMessage = json.getString(ServiceClient.WEB_API_MESSAGE_NODE);
				}
				if(json.has(ServiceClient.WEB_API_MODEL_STATE_NODE)) {
					serviceError.ModelState = json.getString(ServiceClient.WEB_API_MODEL_STATE_NODE);
				}
			} catch(JSONException e) { // unable to process the network response data as json
				// handle the response per the status code, if known
				switch (error.networkResponse.statusCode) {

					// Status Code: 403 - Not Authorized
					case 403:
						serviceError.ErrorMessage = context.getString(R.string.ServiceError_403);
						break;

					// Status Code: 404 - Not Found
					case 404:
						serviceError.ErrorMessage = context.getString(R.string.ServiceError_404);
						break;
				}
			}
		}

		// deliver the failure with the error message
		ServiceAdapter serviceAdapter = serviceRequest.getServiceAdapter();
		if(serviceAdapter != null) {
			serviceAdapter.onFailure(serviceError.ErrorMessage);
			serviceAdapter.onComplete();
		}
	}

	/**
	 * The callback listener for delivering parsed responses
	 */
	private class ResponseListener implements Response.Listener {

		/**
		 * the service request bound to the Volley request
		 */
		ServiceRequest mServiceRequest;

		/**
		 * The default constructor for this listener
		 *
		 * @param serviceRequest the service request bound to the Volley request
		 */
		ResponseListener(ServiceRequest serviceRequest) {
			this.mServiceRequest = serviceRequest;
		}

		/**
		 * Called when a response is received
		 *
		 * @param response	the volley-response received from the volley-request
		 */
		@Override
		public void onResponse(Object response) {

			// set the request time and sleep time in milliseconds
			long requestMillis = (System.currentTimeMillis() - ServiceClient.this.mRequestStartTimeMillis);
			long sleepMillis = Math.max(0, ServiceClient.MINIMUM_REQUEST_TIME_MILLIS - requestMillis);

			// delay processing the response
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {

					// log the response
					if(GlobalSettings.DEBUG) {
						Log.d(LOG_TAG, "::PROCESS RESPONSE::RESPONSE FROM::" + ResponseListener.this.mServiceRequest.getWebApiUrl());
						Log.d(LOG_TAG, "::PROCESS RESPONSE::RESPONSE DELAY:: " +  (float) ServiceClient.MINIMUM_REQUEST_TIME_MILLIS / 1000 + " Seconds");
						Log.d(LOG_TAG, "::PROCESS RESPONSE::RESPONSE RECEIVED:: " + (float) requestMillis / 1000 + " Seconds");
						Log.d(LOG_TAG, "::PROCESS RESPONSE::RESPONSE PAYLOAD::" + JsonHelper.getJsonString(response));
					}

					// process the response
					ServiceClient.this.processResponse(ResponseListener.this.mServiceRequest, response);
				}
			}, sleepMillis);
		}
	}

	/**
	 * The callback listener for delivering error responses
	 */
	private class ErrorListener implements Response.ErrorListener {

		/**
		 * the context of the application
		 */
		private Context mContext;

		/**
		 * the service request bound to the Volley request
		 */
		ServiceRequest mServiceRequest;

		/**
		 * The default constructor for this listener
		 *
		 * @param context           the context of the application
		 * @param serviceRequest    the service request bound to the Volley request
		 */
		ErrorListener(Context context, ServiceRequest serviceRequest) {
			this.mContext = context;
			this.mServiceRequest = serviceRequest;
		}

		/**
		 * Callback method that an error has been occurred with the
		 * provided error code and optional user-readable message.
		 *
		 * @param error	the volley-error received from the volley-request
		 */
		@Override
		public void onErrorResponse(VolleyError error) {
			ServiceClient.this.processResponseError(this.mContext, this.mServiceRequest, error);
		}
	}
}
