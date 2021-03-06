/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.sync.parsing

import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.matrix.android.sdk.internal.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.internal.session.sync.model.SyncResponse
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class InitialSyncResponseParser @Inject constructor(private val moshi: Moshi) {

    fun parse(syncStrategy: InitialSyncStrategy.Optimized, workingFile: File): SyncResponse {
        val syncResponseLength = workingFile.length().toInt()
        Timber.v("INIT_SYNC Sync file size is $syncResponseLength bytes")
        val shouldSplit = syncResponseLength >= syncStrategy.minSizeToSplit
        Timber.v("INIT_SYNC should split in several files: $shouldSplit")
        return getMoshi(syncStrategy, workingFile.parentFile!!, shouldSplit)
                .adapter(SyncResponse::class.java)
                .fromJson(workingFile.source().buffer())!!
    }

    private fun getMoshi(syncStrategy: InitialSyncStrategy.Optimized, workingDirectory: File, shouldSplit: Boolean): Moshi {
        // If we don't have to split we'll rely on the already default moshi
        if (!shouldSplit) return moshi
        // Otherwise, we create a new adapter for handling Map of Lazy sync
        return moshi.newBuilder()
                .add(SplitLazyRoomSyncJsonAdapter(workingDirectory, syncStrategy))
                .build()
    }
}
