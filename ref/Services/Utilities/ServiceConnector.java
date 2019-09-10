package com.connectfss.MobileBanking.Services.Utilities;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import com.connectfss.MobileBanking.Services.Connections.IConnectionListener;
import com.connectfss.MobileBanking.Services.Connections.MobileBankingServiceConnection;
import com.connectfss.MobileBanking.Services.MobileBankingService;
import com.connectfss.MobileBanking.Utilities.GlobalSettings;
import com.connectfss.MobileBanking.Utilities.Log;

/**
 * A Service Connector Singleton object
 *
 * intended expose, bind, unbind, connect, and disconnect all known mobile banking services
 */
public class ServiceConnector {

	/**
	 * the singleton instance
	 */
	private static volatile ServiceConnector instance = null;

	/**
	 * the Mobile Banking service connection
	 */
	private MobileBankingServiceConnection mMobileBankingServiceConnection;

	/**
	 * exists only to defeat instantiation
	 */
	private ServiceConnector() {}

	/**
	 * get the instance of the ServiceConnector singleton
	 *
	 * @param context	the application context to bind to the instance of the ServiceConnector singleton
	 * @return 			the instance of the ServiceConnector singleton
	 */
	public static synchronized ServiceConnector getInstance(Context context) {
		if (instance == null) {
			instance = new ServiceConnector();
			instance.bindServices(context.getApplicationContext());
		}
		return instance;
	}

	/**
	 * destroy the instance of the ServiceConnector singleton
	 */
	public static void destroyInstance(Context context) {
		if (instance != null) {
			instance.unbindServices(context.getApplicationContext());
			instance = null;
		}
	}

	/**
	 * Bind all of Mobile-Banking Service Connections
	 *
	 * @param context the context to bind the the Service Connections to
	 */
	private void bindServices(Context context) {
		this.mMobileBankingServiceConnection = new MobileBankingServiceConnection();
		context.bindService(new Intent(context, MobileBankingService.class), this.mMobileBankingServiceConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Unbind all of Mobile-Banking Service Connections
	 *
	 * @param context the context to unbind the the Service Connections from
	 */
	private void unbindServices(Context context) {
		this.unbindService(context, this.mMobileBankingServiceConnection);
	}

	/**
	 * Unbind a single Service Connection
	 *
	 * @param context the Context to unbind the the Service Connection from
	 * @param serviceConnection the Service Connection to unbind
	 */
	private void unbindService(Context context, ServiceConnection serviceConnection) {
		try {
			context.unbindService(serviceConnection);
		} catch(IllegalArgumentException e) {
			if(GlobalSettings.DEBUG) {
				e.printStackTrace();
			}
			Log.e(e.getMessage());
		}
	}

	/**
	 * Determines if all services handled by the Service Connector are bound and connected
	 *
	 * @return	true or false, whether or not all services are bound and connected
	 */
	public boolean isConnected() {
		return this.mMobileBankingServiceConnection.getService() != null;
	}

	/**
	 * Get the bound Mobile Banking service
	 *
	 * @return the bound Mobile Banking service
	 */
	public final MobileBankingService getMobileBankingService() {
		return this.mMobileBankingServiceConnection.getService();
	}

	/**
	 * Set the mobile-config service connection listener
	 *
	 * @param connectionListener the connection listener to set the mobile-config service connection listener to
	 */
	public final void setMobileBankingServiceConnectionListener(IConnectionListener connectionListener) {
		this.mMobileBankingServiceConnection.setConnectionListener(connectionListener);
	}
}
