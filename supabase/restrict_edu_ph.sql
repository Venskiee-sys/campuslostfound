-- ============================================================
-- UniFind — require school (.edu.ph) email addresses
--
-- The app checks the domain on every form and after the Google account
-- picker, but a modified APK could skip those checks. This hook is the
-- backstop that runs inside Supabase Auth itself, so a new account can
-- only ever be created with an address ending in `.edu.ph` — whether it
-- comes from email/password sign-up or from "Continue with Google".
--
-- Run this ONCE in:  Supabase Dashboard → SQL Editor → New query → Run
-- Then ENABLE it (running the SQL alone changes nothing):
--   Authentication → Hooks → "Before User Created" → Postgres function →
--   select public.hook_restrict_signup_by_email_domain → Create.
--
-- Note: the hook only runs when a NEW user is created, so accounts that
-- already exist (e.g. created while testing with @gmail.com) keep working.
-- ============================================================

create or replace function public.hook_restrict_signup_by_email_domain(event jsonb)
returns jsonb
language plpgsql
as $$
declare
  email  text := lower(trim(coalesce(event->'user'->>'email', '')));
  domain text := split_part(email, '@', 2);
begin
  if domain = 'edu.ph' or domain like '%.edu.ph' then
    return '{}'::jsonb;  -- allow
  end if;

  return jsonb_build_object(
    'error', jsonb_build_object(
      'message', 'Please use your school email ending in .edu.ph (e.g. juan@plmun.edu.ph).',
      'http_code', 403
    )
  );
end;
$$;

-- Only Supabase Auth may execute the hook; nobody logged in through the
-- API can call it directly.
grant execute
  on function public.hook_restrict_signup_by_email_domain(jsonb)
  to supabase_auth_admin;

revoke execute
  on function public.hook_restrict_signup_by_email_domain(jsonb)
  from authenticated, anon, public;
