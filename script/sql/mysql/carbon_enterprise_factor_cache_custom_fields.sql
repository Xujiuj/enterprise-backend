ALTER TABLE ce_factor_cache_record
    ADD COLUMN custom_fields TEXT DEFAULT NULL AFTER source_ref;
