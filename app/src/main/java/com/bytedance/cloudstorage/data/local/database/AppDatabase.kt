package com.bytedance.cloudstorage.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bytedance.cloudstorage.data.local.dao.FileDao
import com.bytedance.cloudstorage.data.local.dao.ShareLinkDao
import com.bytedance.cloudstorage.data.local.dao.TransferRecordDao
import com.bytedance.cloudstorage.data.local.entity.FileEntity
import com.bytedance.cloudstorage.data.local.entity.ShareLinkEntity
import com.bytedance.cloudstorage.data.local.entity.ShareLinkFileEntity
import com.bytedance.cloudstorage.data.local.entity.TransferRecordEntity

/**
 * Room 数据库类
 *
 * entities = [FileEntity::class] 声明该数据库包含的所有表。
 * version = 1 表示数据库版本，后续修改表结构时需要递增并提供 Migration。
 *
 * Room 会在编译期自动生成 AppDatabase_Impl 实现类，
 * 包含建表 SQL 和 DAO 方法的具体实现。
 */
@Database(
    entities = [
        FileEntity::class,
        ShareLinkEntity::class,
        ShareLinkFileEntity::class,
        TransferRecordEntity::class,
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** 提供 FileDao 实例，Room 自动生成实现 */
    abstract fun fileDao(): FileDao
    /** 提供 ShareLinkDao 实例 */
    abstract fun shareLinkDao(): ShareLinkDao
    /** 提供 TransferRecordDao 实例 */
    abstract fun transferRecordDao(): TransferRecordDao

    companion object {
        /**
         * 单例模式：保证整个 App 只有一个数据库实例，避免多线程冲突。
         *
         * @Volatile 保证 instance 的修改对所有线程立即可见。
         * synchronized 确保只有一个线程进入创建逻辑。
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1→v2: 新增视频封面 URI 字段 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN coverUri TEXT")
            }
        }

        /** v2→v3: 新增分享链接相关表（share_links、share_link_files） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS share_links (
                        token TEXT NOT NULL PRIMARY KEY,
                        createdAt INTEGER NOT NULL,
                        handledAt INTEGER,
                        handledAction TEXT,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS share_link_files (
                        token TEXT NOT NULL,
                        fileId TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        PRIMARY KEY(token, fileId),
                        FOREIGN KEY(token) REFERENCES share_links(token) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_share_link_files_token ON share_link_files(token)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_share_link_files_fileId ON share_link_files(fileId)")
            }
        }

        /** v3→v4: 新增传输记录表（transfer_records） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transfer_records (
                        recordId TEXT NOT NULL PRIMARY KEY,
                        fileId TEXT,
                        name TEXT NOT NULL,
                        size INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        direction TEXT NOT NULL,
                        source TEXT NOT NULL,
                        status TEXT NOT NULL,
                        savedPath TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "miniclouddisk.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
