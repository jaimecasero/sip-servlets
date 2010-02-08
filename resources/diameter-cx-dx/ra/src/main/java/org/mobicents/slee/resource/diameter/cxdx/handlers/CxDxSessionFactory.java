package org.mobicents.slee.resource.diameter.cxdx.handlers;

import org.apache.log4j.Logger;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.cxdx.ClientCxDxSession;
import org.jdiameter.api.cxdx.ClientCxDxSessionListener;
import org.jdiameter.api.cxdx.ServerCxDxSession;
import org.jdiameter.api.cxdx.ServerCxDxSessionListener;
import org.jdiameter.api.cxdx.events.JLocationInfoAnswer;
import org.jdiameter.api.cxdx.events.JLocationInfoRequest;
import org.jdiameter.api.cxdx.events.JMultimediaAuthAnswer;
import org.jdiameter.api.cxdx.events.JMultimediaAuthRequest;
import org.jdiameter.api.cxdx.events.JPushProfileAnswer;
import org.jdiameter.api.cxdx.events.JPushProfileRequest;
import org.jdiameter.api.cxdx.events.JRegistrationTerminationAnswer;
import org.jdiameter.api.cxdx.events.JRegistrationTerminationRequest;
import org.jdiameter.api.cxdx.events.JServerAssignmentAnswer;
import org.jdiameter.api.cxdx.events.JServerAssignmentRequest;
import org.jdiameter.api.cxdx.events.JUserAuthorizationAnswer;
import org.jdiameter.api.cxdx.events.JUserAuthorizationRequest;
import org.jdiameter.client.impl.app.cxdx.CxDxClientSessionImpl;
import org.jdiameter.common.api.app.IAppSessionFactory;
import org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory;
import org.jdiameter.common.impl.app.cxdx.JLocationInfoAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JLocationInfoRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JMultimediaAuthAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JMultimediaAuthRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JPushProfileAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JPushProfileRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JRegistrationTerminationAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JRegistrationTerminationRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JServerAssignmentAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JServerAssignmentRequestImpl;
import org.jdiameter.common.impl.app.cxdx.JUserAuthorizationAnswerImpl;
import org.jdiameter.common.impl.app.cxdx.JUserAuthorizationRequestImpl;

/**
 * 
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class CxDxSessionFactory implements IAppSessionFactory, ServerCxDxSessionListener,ClientCxDxSessionListener, StateChangeListener, ICxDxMessageFactory {

  private CxDxSessionCreationListener cxdxResourceAdaptor;
  private SessionFactory sessionFactory;
  private static final Logger logger = Logger.getLogger(CxDxSessionFactory.class);

  public CxDxSessionFactory(CxDxSessionCreationListener cxDxResourceAdaptor, long messageTimeout, SessionFactory sessionFactory) {
    super();
    this.cxdxResourceAdaptor = cxDxResourceAdaptor;
    this.sessionFactory = sessionFactory;
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.IAppSessionFactory#getNewSession(java.lang.String, java.lang.Class, org.jdiameter.api.ApplicationId, java.lang.Object[])
   */
  public AppSession getNewSession(String sessionId, Class<? extends AppSession> appSessionClass, ApplicationId applicationId, Object[] args) {
    AppSession appSession = null;

    if(appSessionClass == ClientCxDxSession.class) {
      CxDxClientSessionImpl clientSession = null;
      if (args != null && args.length > 0 && args[0] instanceof Request) {
        Request request = (Request) args[0];
        clientSession = new CxDxClientSessionImpl(request.getSessionId(),this,this.sessionFactory,this);
      }
      else {
        clientSession = new CxDxClientSessionImpl(sessionId,this,this.sessionFactory,this);
      }

      clientSession.getSessions().get(0).setRequestListener(clientSession);
      clientSession.addStateChangeNotification(this);

      this.cxdxResourceAdaptor.sessionCreated(clientSession);

      appSession = clientSession;
    }
    else if(appSessionClass == ServerCxDxSession.class) {
      org.jdiameter.server.impl.app.cxdx.CxDxServerSessionImpl serverSession = null;
      if (args != null && args.length > 0 && args[0] instanceof Request) {
        // This shouldn't happen but just in case
        Request request = (Request) args[0];
        serverSession = new org.jdiameter.server.impl.app.cxdx.CxDxServerSessionImpl(request.getSessionId(), this, sessionFactory, this);
      }
      else {
        serverSession = new org.jdiameter.server.impl.app.cxdx.CxDxServerSessionImpl(sessionId, this, sessionFactory, this);
      }

      serverSession.addStateChangeNotification(this);
      serverSession.getSessions().get(0).setRequestListener(serverSession);

      this.cxdxResourceAdaptor.sessionCreated(serverSession);

      appSession = serverSession;
    }
    else {
      throw new IllegalArgumentException("Wrong session class!![" + appSessionClass + "]. Supported[" + ServerCxDxSession.class + "," + ClientCxDxSession.class + "]");
    }

    return appSession;
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doLocationInformationRequest(org.jdiameter.api.cxdx.ServerCxDxSession, org.jdiameter.api.cxdx.events.JLocationInfoRequest)
   */
  public void doLocationInformationRequest(ServerCxDxSession appSession, JLocationInfoRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doLocationInformationRequest :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, request.getMessage());
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doMultimediaAuthRequest(org.jdiameter.api.cxdx.ServerCxDxSession, org.jdiameter.api.cxdx.events.JMultimediaAuthRequest)
   */
  public void doMultimediaAuthRequest(ServerCxDxSession appSession, JMultimediaAuthRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doMultimediaAuthRequest :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, request.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doOtherEvent(org.jdiameter.api.app.AppSession, org.jdiameter.api.app.AppRequestEvent, org.jdiameter.api.app.AppAnswerEvent)
   */
  public void doOtherEvent(AppSession appSession, AppRequestEvent request, AppAnswerEvent answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doOtherEvent :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer != null ? answer.getMessage() : request.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doPushProfileAnswer(org.jdiameter.api.cxdx.ServerCxDxSession, org.jdiameter.api.cxdx.events.JPushProfileRequest, org.jdiameter.api.cxdx.events.JPushProfileAnswer)
   */
  public void doPushProfileAnswer(ServerCxDxSession appSession, JPushProfileRequest request, JPushProfileAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doPushProfileAnswer :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doRegistrationTerminationAnswer(org.jdiameter.api.cxdx.ServerCxDxSession, org.jdiameter.api.cxdx.events.JRegistrationTerminationRequest, org.jdiameter.api.cxdx.events.JRegistrationTerminationAnswer)
   */
  public void doRegistrationTerminationAnswer(ServerCxDxSession appSession, JRegistrationTerminationRequest request, JRegistrationTerminationAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doRegistrationTerminationAnswer :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer.getMessage());
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doServerAssignmentRequest(org.jdiameter.api.cxdx.ServerCxDxSession, org.jdiameter.api.cxdx.events.JServerAssignmentRequest)
   */
  public void doServerAssignmentRequest(ServerCxDxSession appSession, JServerAssignmentRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doServerAssignmentRequest :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, request.getMessage());
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ServerCxDxSessionListener#doUserAuthorizationRequest(org.jdiameter.api.cxdx.ServerCxDxSession, org.jdiameter.api.cxdx.events.JUserAuthorizationRequest)
   */
  public void doUserAuthorizationRequest(ServerCxDxSession appSession, JUserAuthorizationRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doUserAuthorizationRequest :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, request.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ClientCxDxSessionListener#doLocationInformationAnswer(org.jdiameter.api.cxdx.ClientCxDxSession, org.jdiameter.api.cxdx.events.JLocationInfoRequest, org.jdiameter.api.cxdx.events.JLocationInfoAnswer)
   */
  public void doLocationInformationAnswer(ClientCxDxSession appSession, JLocationInfoRequest request, JLocationInfoAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doLocationInformationAnswer :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ClientCxDxSessionListener#doMultimediaAuthAnswer(org.jdiameter.api.cxdx.ClientCxDxSession, org.jdiameter.api.cxdx.events.JMultimediaAuthRequest, org.jdiameter.api.cxdx.events.JMultimediaAuthAnswer)
   */
  public void doMultimediaAuthAnswer(ClientCxDxSession appSession, JMultimediaAuthRequest request, JMultimediaAuthAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doMultimediaAuthAnswer :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer.getMessage());
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ClientCxDxSessionListener#doPushProfileRequest(org.jdiameter.api.cxdx.ClientCxDxSession, org.jdiameter.api.cxdx.events.JPushProfileRequest)
   */
  public void doPushProfileRequest(ClientCxDxSession appSession, JPushProfileRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doPushProfileRequest :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, request.getMessage());
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ClientCxDxSessionListener#doRegistrationTerminationRequest(org.jdiameter.api.cxdx.ClientCxDxSession, org.jdiameter.api.cxdx.events.JRegistrationTerminationRequest)
   */
  public void doRegistrationTerminationRequest(ClientCxDxSession appSession, JRegistrationTerminationRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doRegistrationTerminationRequest :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, request.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ClientCxDxSessionListener#doServerAssignmentAnswer(org.jdiameter.api.cxdx.ClientCxDxSession, org.jdiameter.api.cxdx.events.JServerAssignmentRequest, org.jdiameter.api.cxdx.events.JServerAssignmentAnswer)
   */
  public void doServerAssignmentAnswer(ClientCxDxSession appSession, JServerAssignmentRequest request, JServerAssignmentAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doServerAssignmentAnswer :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.cxdx.ClientCxDxSessionListener#doUserAuthorizationAnswer(org.jdiameter.api.cxdx.ClientCxDxSession, org.jdiameter.api.cxdx.events.JUserAuthorizationRequest, org.jdiameter.api.cxdx.events.JUserAuthorizationAnswer)
   */
  public void doUserAuthorizationAnswer(ClientCxDxSession appSession, JUserAuthorizationRequest request, JUserAuthorizationAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    logger.info("Diameter Cx/Dx Session Factory :: doUserAuthorizationAnswer :: appSession[" + appSession + "], Request[" + request + "]");

    doFireEvent(appSession, answer.getMessage());
  }

  /* (non-Javadoc)
   * @see org.jdiameter.api.app.StateChangeListener#stateChanged(java.lang.Enum, java.lang.Enum)
   */
  public void stateChanged(Enum oldState, Enum newState) {
    logger.info("Diameter Cx/Dx Session Factory :: stateChanged :: oldState[" + oldState + "], newState[" + newState + "]");
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createLocationInfoAnswer(org.jdiameter.api.Answer)
   */
  public AppAnswerEvent createLocationInfoAnswer(Answer answer) {
    return new JLocationInfoAnswerImpl(answer);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createLocationInfoRequest(org.jdiameter.api.Request)
   */
  public AppRequestEvent createLocationInfoRequest(Request request) {
    return new JLocationInfoRequestImpl(request);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createMultimediaAuthAnswer(org.jdiameter.api.Answer)
   */
  public AppAnswerEvent createMultimediaAuthAnswer(Answer answer) {
    return new JMultimediaAuthAnswerImpl(answer);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createMultimediaAuthRequest(org.jdiameter.api.Request)
   */
  public AppRequestEvent createMultimediaAuthRequest(Request request) {
    return new JMultimediaAuthRequestImpl(request);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createPushProfileAnswer(org.jdiameter.api.Answer)
   */
  public AppAnswerEvent createPushProfileAnswer(Answer answer) {
    return new JPushProfileAnswerImpl(answer);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createPushProfileRequest(org.jdiameter.api.Request)
   */
  public AppRequestEvent createPushProfileRequest(Request request) {
    return new JPushProfileRequestImpl(request);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createRegistrationTerminationAnswer(org.jdiameter.api.Answer)
   */
  public AppAnswerEvent createRegistrationTerminationAnswer(Answer answer) {
    return new JRegistrationTerminationAnswerImpl(answer);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createRegistrationTerminationRequest(org.jdiameter.api.Request)
   */
  public AppRequestEvent createRegistrationTerminationRequest(Request request) {
    return new JRegistrationTerminationRequestImpl(request);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createServerAssignmentAnswer(org.jdiameter.api.Answer)
   */
  public AppAnswerEvent createServerAssignmentAnswer(Answer answer) {
    return new JServerAssignmentAnswerImpl(answer);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createServerAssignmentRequest(org.jdiameter.api.Request)
   */
  public AppRequestEvent createServerAssignmentRequest(Request request) {
    return new JServerAssignmentRequestImpl(request);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createUserAuthorizationAnswer(org.jdiameter.api.Answer)
   */
  public AppAnswerEvent createUserAuthorizationAnswer(Answer answer) {
    return new JUserAuthorizationAnswerImpl(answer);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#createUserAuthorizationRequest(org.jdiameter.api.Request)
   */
  public AppRequestEvent createUserAuthorizationRequest(Request request) {
    return new JUserAuthorizationRequestImpl(request);
  }

  /* (non-Javadoc)
   * @see org.jdiameter.common.api.app.cxdx.ICxDxMessageFactory#getApplicationId()
   */
  public long getApplicationId() {
    // FIXME: Not needed?
    return 16777216;
  }

  private void doFireEvent(AppSession appSession, Message message) {
    this.cxdxResourceAdaptor.fireEvent(appSession.getSessions().get(0).getSessionId(), message);
  }
}
