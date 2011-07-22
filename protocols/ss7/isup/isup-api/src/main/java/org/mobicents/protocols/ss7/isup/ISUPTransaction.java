/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.isup;

import org.mobicents.protocols.ss7.isup.message.ISUPMessage;

/**
 * Start time:08:56:08 2009-08-30<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 */
public interface ISUPTransaction {
	/**
	 * Get unique transaction key associated with this transaction
	 * @return
	 */
	public TransactionKey getTransactionKey();
	/**
	 * Determine if this transaction is server.
	 * @return
	 */
	public boolean isServerTransaction();
	/**
	 * Get original message whcih started this transaction
	 * @return
	 */
	public ISUPMessage getOriginalMessage();
	/**
	 * Determine if transaction has terminated properly.
	 * @return
	 */
	public boolean isTerminated();
	/**
	 * Determine if transaction has terminated properly.
	 * @return
	 */
	public boolean isTimedout();
	
}