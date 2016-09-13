/**
 * Copyright 2014-2016 yangming.liu<bytefox@126.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytetcc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.Xid;

import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.archive.CompensableArchive;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;
import org.bytesoft.compensable.logging.CompensableLogger;
import org.bytesoft.transaction.Transaction;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionRecovery;
import org.bytesoft.transaction.archive.TransactionArchive;
import org.bytesoft.transaction.recovery.TransactionRecoveryCallback;
import org.bytesoft.transaction.recovery.TransactionRecoveryListener;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionRecoveryImpl
		implements TransactionRecovery, TransactionRecoveryListener, CompensableBeanFactoryAware {
	static final Logger logger = LoggerFactory.getLogger(TransactionRecoveryImpl.class.getSimpleName());

	private CompensableBeanFactory beanFactory;

	private final Map<TransactionXid, Transaction> recovered = new HashMap<TransactionXid, Transaction>();

	public void onRecovery(Transaction transaction) {
		TransactionContext transactionContext = transaction.getTransactionContext();
		TransactionXid xid = transactionContext.getXid();

		XidFactory xidFactory = this.beanFactory.getCompensableXidFactory();
		TransactionXid globalXid = xidFactory.createGlobalXid(xid.getGlobalTransactionId());

		this.recovered.put(globalXid, transaction);
	}

	public void startRecovery() {
		this.fireTransactionStartRecovery();
		this.fireCompensableStartRecovery();
	}

	private void fireTransactionStartRecovery() {
		TransactionRecovery transactionRecovery = this.beanFactory.getTransactionRecovery();
		transactionRecovery.startRecovery();
	}

	private void fireCompensableStartRecovery() {
		CompensableLogger compensableLogger = this.beanFactory.getCompensableLogger();
		compensableLogger.recover(new TransactionRecoveryCallback() {
			public void recover(TransactionArchive archive) {
				this.recover((org.bytesoft.compensable.archive.TransactionArchive) archive);
			}

			public void recover(org.bytesoft.compensable.archive.TransactionArchive archive) {
				XidFactory xidFactory = beanFactory.getCompensableXidFactory();

				CompensableTransactionImpl transaction = reconstructTransaction(archive);
				int transactionStatus = archive.getStatus();
				int compensableStatus = archive.getCompensableStatus();

				List<CompensableArchive> compensableArchiveList = archive.getCompensableResourceList();
				for (int i = 0; i < compensableArchiveList.size(); i++) {
					CompensableArchive compensableArchive = compensableArchiveList.get(i);
					Xid transactionXid = compensableArchive.getTransactionXid();
					Xid compensableXid = compensableArchive.getCompensableXid();
					TransactionXid xid1 = xidFactory.createGlobalXid(transactionXid.getBranchQualifier());
					TransactionXid xid2 = xidFactory.createGlobalXid(compensableXid.getBranchQualifier());

					Transaction tx = recovered.get(xid1);
				}

			}
		});
	}

	public CompensableTransactionImpl reconstructTransaction(TransactionArchive archive) {
		return null;
	}

	public void timingRecover() {
		// TODO Auto-generated method stub

	}

	public void setBeanFactory(CompensableBeanFactory tbf) {
		this.beanFactory = tbf;
	}

}
