/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.slee.resource.diameter.base;

import java.io.IOException;

import net.java.slee.resource.diameter.base.AccountingClientSessionActivity;
import net.java.slee.resource.diameter.base.AccountingSessionState;
import net.java.slee.resource.diameter.base.DiameterAvpFactory;
import net.java.slee.resource.diameter.base.DiameterMessageFactory;
import net.java.slee.resource.diameter.base.events.AccountingRequest;
import net.java.slee.resource.diameter.base.events.avp.AccountingRecordType;
import net.java.slee.resource.diameter.base.events.avp.AvpNotAllowedException;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;

import org.jdiameter.api.Answer;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.acc.ClientAccSession;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.common.api.app.acc.ClientAccSessionState;
import org.jdiameter.common.impl.app.acc.AccountRequestImpl;
import org.jdiameter.common.impl.validation.JAvpNotAllowedException;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;

/**
 * 
 * Implementation of {@link AccountingClientSessionActivity}.
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class AccountingClientSessionActivityImpl extends AccountingSessionActivityImpl implements AccountingClientSessionActivity {

  private static final long serialVersionUID = -4377919257333940587L;

  protected transient ClientAccSession clientSession = null;

  public AccountingClientSessionActivityImpl(DiameterMessageFactory messageFactory, DiameterAvpFactory avpFactory, ClientAccSession clientSession, DiameterIdentity destinationHost, DiameterIdentity destinationRealm) {
    super(messageFactory, avpFactory, null,(EventListener<Request, Answer>) clientSession, destinationHost, destinationRealm);

    setSession(clientSession);

    super.setCurrentWorkingSession(this.clientSession.getSessions().get(0));
  }

  public AccountingRequest createAccountingRequest(AccountingRecordType accountingRecordType) {
    AccountingRequest acr = messageFactory.createAccountingRequest();

    // Set Acct-Application-Id to 3 as specified
    acr.setAcctApplicationId(3L);
    acr.setAccountingRecordType(accountingRecordType);

    return acr;
  }

  public void sendAccountRequest(AccountingRequest request) throws IOException {
    DiameterMessageImpl msg = (DiameterMessageImpl) request;
    try {
      this.clientSession.sendAccountRequest(new AccountRequestImpl((Request) msg.getGenericData()));
    }
    catch (JAvpNotAllowedException e) {
      throw new AvpNotAllowedException("Message validation failed.", e, e.getAvpCode(), e.getVendorId());
    }
    catch (Exception e) {
      if(logger.isDebugEnabled()) {
        logger.debug("Failed to send message, due to: ", e);
      }
      throw new IOException("Failed to send message, due to: " + e);
    }
  }

  public void stateChanged(AppSession source, Enum oldState, Enum newState) {
    stateChanged(oldState, newState);
  }

  public void stateChanged(Enum oldState, Enum newState) {
    ClientAccSessionState state = (ClientAccSessionState) newState;

    //FIXME: baranowb: PendingL - where does this fit?
    switch (state) {
    case IDLE:
      if(oldState != state) {
        //String sessionId = this.clientSession.getSessions().get(0).getSessionId();
        //this.state = AccountingSessionState.Idle;
        //this.clientSession.release();
        //this.baseListener.sessionDestroyed(sessionId, this.clientSession);
        endActivity();
      }
      break;
    case OPEN:
      //this.state = AccountingSessionState.Open;
      break;
    case PENDING_EVENT:
      // this.state = AccountingSessionState.PendingE;
      break;
    case PENDING_START:
      //this.state = AccountingSessionState.PendingS;
      break;
    case PENDING_INTERIM:
      // this.state = AccountingSessionState.PendingI;
      break;
    case PENDING_CLOSE:
      // this.state = AccountingSessionState.PendingS;
      break;
    case PENDING_BUFFERED:
      //this.state = AccountingSessionState.PendingB;
      break;
    }
  }

  public ClientAccSession getSession() {
    return this.clientSession;
  }

  @Override
  public AccountingSessionState getAccountingSessionState() {
    ClientAccSessionState state = (ClientAccSessionState) clientSession.getState(ClientAccSessionState.class);

    //FIXME: baranowb: PendingL - where does this fit?
    switch (state) {
    case IDLE:
      return AccountingSessionState.Idle;
    case OPEN:
      return AccountingSessionState.Open;
    case PENDING_EVENT:
      return AccountingSessionState.PendingE;
    case PENDING_START:
      return AccountingSessionState.PendingS;
    case PENDING_INTERIM:
      return AccountingSessionState.PendingI;
    case PENDING_CLOSE:
      return AccountingSessionState.PendingC;
    case PENDING_BUFFERED:
      return AccountingSessionState.PendingB;
    default:
      logger.error("Unexpected state in Accounting Client FSM: " + state);
      return null;
    }
  }

  //used to recreate object.
  public void setSession(ClientAccSession clientSession) {
    this.clientSession = clientSession;
    this.clientSession.addStateChangeNotification(this);
    super.eventListener = (EventListener<Request, Answer>) clientSession;
  }

  @Override
  public void endActivity() {
    this.clientSession.release();
    super.baseListener.endActivity(getActivityHandle());
  }

}